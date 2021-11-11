package hackathon

import github.{GitHubAPI, WebhookPayload}
import hackathon.api.{Issue, IssueAPI, IssueId}
import sttp.client3.httpclient.zio.{HttpClientZioBackend, SttpClient, send}
import sttp.client3.{Response => _, _}
import zhttp.http._
import zhttp.service.server.ServerChannelFactory
import zhttp.service.{EventLoopGroup, Server}
import zio._
import zio.app.DeriveRoutes
import zio.json._
import zio.magic._

/** 1. Redirect your user to some special url. https://github.com/oauth?client_id=12345&redirect_url=zio-hackathon.com/something
  * 2. Github redirects the user back to your site with a ?token=some-magic-token
  * 3. You use that token to hit github.com/oauth/access-token?client_id&client_secret&token
  * 4. The actual access token
  *    Authorization "token $token"
  * 5. Get the users Id and Login
  *  ----
  * 5. redirect the user back to our frontend. frontend.com?login=123456
  * 6. javascript web token "{ userId: 1234 }"
  */

// https://github.com/login/oauth/authorize?client_id=c7602bfac6b2ca51615f&scope=public_repo

object Backend extends App {
  val webhooks =
    HttpApp.collectM { case req @ Method.POST -> Root / "github" / "webhooks" =>
      req.getBodyAsString
        .flatMap(_.fromJson[WebhookPayload].toOption)
        .map {
          case WebhookPayload.IssueCommentWebhook(action, sender, issue, comment) =>
            println("=== ISSUE COMMENT WEBHOOK ===")
            println(action)
            println(sender)
            println(PrettyPrint(issue))
            println(PrettyPrint(comment))

            IssueRepository
              .claim(IssueId(issue.id), sender.login)
              .when(comment.body.contains("I will look into this"))
              .as(Response.ok)

          case WebhookPayload.IssueWebhook(action, sender, issue) =>
            println("=== ISSUE WEBHOOK ===")
            println(action)
            println(sender)
            println(PrettyPrint(issue))

            IssueRepository
              .save(
                List(
                  Issue(
                    IssueId(issue.id),
                    issue.number,
                    issue.title,
                    issue.body,
                    "kitlangton",
                    "zio-hackathon",
                    issue.url,
                    None
                  )
                )
              )
              .when(issue.labels.exists(_.name == "zio-hackathon"))
              .as(Response.ok)
        }
        .getOrElse {
          println("=== UNMATCHED WEBHOOK ===")
          println(req.getBodyAsString)
          UIO(Response.ok)
        }
    }

  val oauth =
    HttpApp.collectM { case req @ Method.GET -> Root / "oauth" / "github" =>
      println(s"Received: $req")
      val authCode = req.url.queryParams("code").head
      for {
        token <- requestAccessToken(authCode)
        user  <- GitHubAPI.user(token)
        _     <- IssueRepository.saveUser(login = user.login, id = user.id, token = token)
      } yield Response.http(
        Status.TEMPORARY_REDIRECT,
        List(Header.custom("Location", s"http://localhost:3000?githubUser=${user.id}"))
      )
    }

  private val httpApp =
    DeriveRoutes.gen[IssueAPI] +++ webhooks +++ oauth

  val program = for {
    _ <- IssueRepository.issueStream
      .foreach(issue =>
        ZIO.debug("ISSUE STREAM EVENT") *>
          ZIO.debug(PrettyPrint(issue))
      )
      .fork
    _ <- IssueRepository.claimsStream
      .foreach(claim =>
        ZIO.debug("CLAIMS STREAM EVENT") *>
          ZIO.debug(PrettyPrint(claim))
      )
      .fork
    port <- system.envOrElse("PORT", "8088").map(_.toInt).orElseSucceed(8088)
    _    <- (Server.port(port) ++ Server.app(httpApp) ++ Server.maxRequestSize(999999)).make.useForever
  } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    program
      .injectCustom(
        GitHubAPI.live,
        QuillContext.live,
        IssueRepository.live,
        IssueAPILive.layer,
        Config.live,
        EventLoopGroup.auto(0),
        ServerChannelFactory.auto,
        HttpClientZioBackend.layer().orDie
      )
      .exitCode
  }

  def requestAccessToken(
      authCode: String
  ): ZIO[SttpClient with Has[GitHubConfig] with Has[GitHubAPI], Throwable, String] = {
    for {
      config <- ZIO.service[GitHubConfig]
      url = uri"https://github.com/login/oauth/access_token"
      params =
        Map(
          "client_id"     -> config.clientId,
          "client_secret" -> config.clientSecret,
          "code"          -> authCode
        )
      resp <- send(quickRequest.post(url).body(params))
      params = parseFormEncoded(resp.body)
      token <- ZIO.fromOption(params.get("access_token")) orElseFail new Error("Could not parse access token")
    } yield token
  }

  def parseFormEncoded(string: String): Map[String, String] =
    string
      .split("&")
      .map { part =>
        val List(key, value) = part.split("=").toList
        (key, value)
      }
      .toMap

}
