package hackathon

import animus._
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L._
import hackathon.api._
import zio.UIO
import zio.app.DeriveClient
import zio.interop.laminar._

import java.util
import scala.collection.immutable.SortedMap

object Colors {
  val orange  = "#F97316"
  val gray100 = "#FAFAF9"
  val gray400 = "#A8A29E"
  val gray500 = "#78716C"
}

object Frontend {
  val client = DeriveClient.gen[IssueAPI]

  val issuesVar   = Var(SortedMap.empty[Long, Issue])
  val projectTags = TagSection("PROJECTS", projects)

  def view: Div = {
    div(
      client.issues.toEventStream --> { issues =>
        val map = SortedMap.from(issues.map { i => i.id.id -> i })
        issuesVar.set(map)
      },
      client.issueStream.toEventStream --> { issue =>
        issuesVar.update(_.updated(issue.id.id, issue))
      },
      cls("main-grid"),
      div(
        cls("sidebar"),
        div(
          Title.amend(marginBottom("60px")),
          projectTags,
          TagSection("TAGS", tags)
        )
      ),
      div(
        cls("main"),
        SectionTitle,
        IssuesView
      )
    )
  }

  val $visibleIssues: Signal[List[Issue]] =
    projectTags.selectedTags.signal.combineWithFn(issuesVar.signal) { (tags, issues) =>
      if (tags.isEmpty) issues.values.toList
      else issues.values.filter(i => tags(i.repo.toUpperCase)).toList
    }

  def heightDynamic($isVisible: Signal[Boolean]): Mod[HtmlElement] = Seq(
    overflowY.hidden,
    onMountBind { (el: MountContext[HtmlElement]) =>
      lazy val $height =
        EventStream
          .periodic(100)
          .mapTo(el.thisNode.ref.scrollHeight.toDouble)
          .toSignal(0.0)

      maxHeight <-- $isVisible.flatMap { b =>
        if (b) { $height }
        else Val(0.0)
      }.spring.px
    }
  )

  def IssuesView: Div =
    div(
      div(
        cls("gradient")
      ),
      children <-- $visibleIssues.splitTransition(_.id) { (_, _, $issue, transition) =>
        div(
          IssueView($issue),
          heightDynamic(transition.$isActive),
          transition.opacity
        )
      }
    )

  lazy val projects: List[String] =
    List("ZIO", "ZIO-HTTP", "ZIO-CACHE", "ZIO-CLI", "CALIBAN", "ZIO-HACKATHON")

  lazy val tags: List[String] =
    List("EASY", "MEDIUM", "HARD")

  private def SectionTitle: Div =
    div(
      "ISSUES.",
      zIndex(2),
      background("#1C1917"),
      position("sticky"),
      top("60px"),
      letterSpacing("0.15em"),
      fontSize("48px"),
      fontWeight.bold,
      color(Colors.gray500),
      marginBottom("60px")
    )

  private def Title: Div =
    div(
      fontWeight(900),
      fontSize("64px"),
      div(color(Colors.orange), "ZIO"),
      div(
        color(Colors.gray100),
        div("HACK-"),
        div("A-"),
        div("THON")
      )
    )

}
