package github

import zio.json.{DeriveJsonDecoder, JsonDecoder}

sealed trait WebhookAction

object WebhookAction {
  case object Opened extends WebhookAction
  case object Closed extends WebhookAction

  case object Created extends WebhookAction
  case object Edited  extends WebhookAction
  case object Deleted extends WebhookAction

  implicit val codec: JsonDecoder[WebhookAction] = JsonDecoder.string.mapOrFail {
    case "closed"  => Right(Closed)
    case "opened"  => Right(Opened)
    case "created" => Right(Created)
    case "edited"  => Right(Edited)
    case "deleted" => Right(Deleted)
    case other     => Left(s"Invalid WebhookAction $other")
  }
}

sealed trait WebhookPayload

object WebhookPayload {
  final case class IssueCommentWebhook(
      action: WebhookAction,
      sender: GithubUser,
      comment: CommentPayload
  ) extends WebhookPayload

  object IssueCommentWebhook {
    implicit val codec: JsonDecoder[IssueCommentWebhook] = DeriveJsonDecoder.gen
  }

  final case class IssueWebhook(
      action: WebhookAction,
      sender: GithubUser,
      issue: IssuePayload
  ) extends WebhookPayload

  object IssueWebhook {
    implicit val codec: JsonDecoder[IssueWebhook] = DeriveJsonDecoder.gen
  }

  implicit val codec: JsonDecoder[WebhookPayload] =
    JsonDecoder[IssueCommentWebhook].widen
      .orElse(JsonDecoder[IssueWebhook].widen)

}

final case class IssuePayload(
    id: Long,
    number: Int,
    title: String,
    body: Option[String],
    url: String,
    labels: List[LabelPayload]
)

object IssuePayload {
  implicit val codec: JsonDecoder[IssuePayload] = DeriveJsonDecoder.gen
}

final case class CommentPayload(body: String)

object CommentPayload {
  implicit val codec: JsonDecoder[CommentPayload] = DeriveJsonDecoder.gen
}

final case class LabelPayload(id: Long, name: String, color: String, url: String)

object LabelPayload {
  implicit val codec: JsonDecoder[LabelPayload] = DeriveJsonDecoder.gen
}

final case class GithubUser(login: String, id: Long)

object GithubUser {
  implicit val codec: JsonDecoder[GithubUser] =
    DeriveJsonDecoder.gen
}
