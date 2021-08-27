package hackathon

import github.WebhookPayload
import hackathon.api.{Issue, IssueAPI, IssueId}
import zhttp.http._
import zhttp.service.server.ServerChannelFactory
import zhttp.service.{EventLoopGroup, Server}
import zio._
import zio.duration._
import zio.app.DeriveRoutes
import zio.clock.Clock
import zio.magic._
import zio.stream.UStream
import zio.json._

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

  private val httpApp =
    DeriveRoutes.gen[IssueAPI] +++ webhooks

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
        QuillContext.live,
        IssueRepository.live,
        IssueAPILive.layer,
        EventLoopGroup.auto(0),
        ServerChannelFactory.auto
      )
      .exitCode
  }
}
