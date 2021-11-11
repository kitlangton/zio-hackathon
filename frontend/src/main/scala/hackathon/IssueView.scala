package hackathon

import com.raquo.laminar.api.L._
import animus._
import hackathon.Frontend.client
import hackathon.api.{Issue, IssueId}
import zio.interop.laminar._

object Claimants {
  val claimants = Var(Map.empty[IssueId, String])
}

final case class IssueView($issue: Signal[Issue]) extends Component {
//  val $claimant: Signal[Option[String]] = Claimants.claimants.signal.map { cs =>
//    cs.get(issue.id)
//  }

  override def body: HtmlElement =
    div(
      cls("issue"),
      composeEvents(onClick)(_.sample($issue)) --> { issue =>
        val token = 7587245
        client.claim(issue.owner, issue.repo, issue.number, token).toEventStream
      },
      div(
        cls("issue-title"),
        child.text <-- $issue.map(_.title)
      ),
      div(
        cls("issue-info"),
        div(
          cls("issue-project"),
          child.text <-- $issue.map(_.repo)
        ),
        div(
          cls("issue-number"),
          child.text <-- $issue.map { i => s"#${i.number}" }
        ),
        Icons.link
      ),
//      children <-- $claimant.map(_.toList).splitTransition(identity) { (_, claimant, _, transition) =>
      child <-- $issue.map(_.claimant).map {
        case Some(claimant) =>
          div(
            cls("issue-claim"),
            div("CLAIMED BY"),
            div(cls("issue-claimant"), claimant)
          )
        case None =>
          div()
      }
//      }
    )
}
