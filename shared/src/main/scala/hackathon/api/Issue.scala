package hackathon.api

final case class IssueId(id: Long) extends AnyVal

final case class Issue(
    id: IssueId,
    number: Int,
    title: String,
    body: Option[String],
    owner: String,
    repo: String,
    url: String,
    claimant: Option[String]
)

object Issue {}
