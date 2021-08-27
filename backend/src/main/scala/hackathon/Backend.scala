package hackathon

import github.WebhookPayload
import hackathon.api.{Issue, IssueAPI}
import zhttp.http._
import zhttp.service.server.ServerChannelFactory
import zhttp.service.{EventLoopGroup, Server}
import zio._
import zio.app.DeriveRoutes
import zio.magic._
import zio.stream.UStream
import zio.json._

object Backend extends App {
  val webhooks = HttpApp.collectM { case req @ Method.POST -> Root / "github" / "webhooks" =>
    req.getBodyAsString
      .flatMap(_.fromJson[WebhookPayload].toOption)
      .map {
        case WebhookPayload.IssueCommentWebhook(action, sender, comment) =>
          println("=== ISSUE COMMENT WEBHOOK ===")
          println(action)
          println(sender)
          println(PrettyPrint(comment))
          UIO(Response.ok)
        case WebhookPayload.IssueWebhook(action, sender, issue) =>
          println("=== ISSUE WEBHOOK ===")
          println(action)
          println(sender)
          println(PrettyPrint(issue))
          UIO(Response.ok)
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
    port <- system.envOrElse("PORT", "8088").map(_.toInt).orElseSucceed(8088)
    _    <- (Server.port(port) ++ Server.app(httpApp) ++ Server.maxRequestSize(999999)).make.useForever
  } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    program
      .injectCustom(IssueAPILive.layer, EventLoopGroup.auto(0), ServerChannelFactory.auto)
      .exitCode
  }
}
