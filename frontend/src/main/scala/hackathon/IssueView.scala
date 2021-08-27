package hackathon

import com.raquo.laminar.api.L._
import animus._
import hackathon.api.{Issue, IssueId}

object Claimants {
  val claimants = Var(Map.empty[IssueId, String])
}

final case class IssueView(issue: Issue) extends Component {
  val $claimant: Signal[Option[String]] = Claimants.claimants.signal.map { cs =>
    cs.get(issue.id)
  }

  override def body: HtmlElement =
    div(
      cls("issue"),
      onClick --> { _ =>
        Claimants.claimants.update {
          _.updatedWith(issue.id) {
            case Some(_) => None
            case None    => Some("kitlangton")
          }
        }
      },
      div(
        cls("issue-title"),
        issue.title
      ),
      div(
        cls("issue-info"),
        div(
          cls("issue-project"),
          issue.repo
        ),
        div(
          cls("issue-number"),
          s"#${issue.number}"
        ),
        Icons.link
      ),
      children <-- $claimant.map(_.toList).splitTransition(identity) { (_, claimant, _, transition) =>
        div(
          cls("issue-claim"),
          div("CLAIMED BY"),
          div(cls("issue-claimant"), claimant),
          transition.height,
          transition.opacity
        )
      }
    )
}
