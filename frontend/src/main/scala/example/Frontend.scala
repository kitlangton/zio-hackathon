package example

import com.raquo.laminar.api.L._
import example.protocol.{ExampleService}
import zio._
import zio.app.DeriveClient
import animus._

final case class Issue(
    title: String,
    project: String,
    number: Int
)

object Issue {
  val examples = List(
    Issue(
      "ZIO 2.0: Use consistent naming convention in Gen.*",
      "ZIO",
      5441
    ),
    Issue(
      "Use javascript weak references",
      "ZIO",
      5442
    ),
    Issue(
      "Add support for enumeratum",
      "Caliban",
      623
    )
  )
}

final case class IssueView(issue: Issue) extends Component {
  override def body: HtmlElement =
    div(
      cls("issue"),
      div(
        cls("issue-title"),
        issue.title
      ),
      div(
        cls("issue-info"),
        div(
          cls("issue-project"),
          issue.project
        ),
        div(
          cls("issue-number"),
          s"#${issue.number}"
        ),
        Icons.link
      )
    )
}

object Colors {
  val orange  = "#F97316"
  val gray100 = "#FAFAF9"
  val gray400 = "#A8A29E"
  val gray500 = "#78716C"
}

final case class TagSection(title: String, tags: List[String]) extends Component {
  val selectedTags =
    Var(Set.empty[String])

  val $anyTagSelected =
    selectedTags.signal.map(_.nonEmpty)

  override def body: HtmlElement =
    div(
      TagHeader(title),
      div(
        display.flex,
        flexWrap.wrap,
        marginBottom("60px"),
        tags.map(TagView)
      )
    )

  private def TagView(name: String): Div = {
    val $isSelected =
      selectedTags.signal.map(_.contains(name))

    div(
      cls("tag"),
      name,
      onClick --> { _ =>
        selectedTags.update { tags =>
          if (tags(name)) tags - name
          else tags + name
        }
      },
      cls.toggle("selected") <-- $isSelected,
      cls.toggle("deselected") <--
        $isSelected.combineWithFn($anyTagSelected) { (selected, any) =>
          !selected && any
        },
      marginRight("24px"),
      fontWeight(800),
      fontSize("18px"),
      marginBottom("18px"),
      letterSpacing("0.15em")
    )
  }

  private def TagHeader(name: String): Div =
    div(
      name,
      letterSpacing("0.15em"),
      fontWeight(300),
      color(Colors.gray400),
      fontSize("16px"),
      marginBottom("24px")
    )

}

object Frontend {
  val runtime = Runtime.default
  val client  = DeriveClient.gen[ExampleService]

  def view: Div = {
    div(
      cls("main-grid"),
      div(
        cls("sidebar"),
        Title.amend(marginBottom("60px")),
        TagSection("PROJECTS", projects),
        TagSection("TAGS", tags)
      ),
      div(
        cls("main"),
        SectionTitle,
        Issue.examples.map(IssueView)
      )
    )
  }

  lazy val projects = List(
    "ZIO",
    "ZIO-HTTP",
    "ZIO-CACHE",
    "ZIO-CLI",
    "CALIBAN"
  )

  lazy val tags =
    List("EASY", "MEDIUM", "HARD")

  private def SectionTitle: Div =
    div(
      "ISSUES.",
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

  private def debugView[A](name: String, effect: => UIO[A]): Div = {
    val output = Var(List.empty[String])
    div(
      h3(name),
      children <-- output.signal.map { strings =>
        strings.map(div(_))
      },
      onClick --> { _ =>
        runtime.unsafeRunAsync_ {
          effect.tap { a => UIO(output.update(_.prepended(a.toString))) }
        }
      }
    )
  }
}

object Icons {
  def link =
    div(
      onMountCallback { el =>
        el.thisNode.ref.innerHTML = """<svg width="14" height="15" viewBox="0 0 14 15" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M8 0.5C7.73479 0.5 7.48043 0.605357 7.2929 0.792893C7.10536 0.98043 7 1.23478 7 1.5C7 1.76522 7.10536 2.01957 7.2929 2.20711C7.48043 2.39464 7.73479 2.5 8 2.5H10.586L4.29301 8.793C4.19749 8.88525 4.12131 8.99559 4.0689 9.1176C4.01649 9.2396 3.98891 9.37082 3.98775 9.5036C3.9866 9.63638 4.0119 9.76806 4.06218 9.89095C4.11246 10.0139 4.18672 10.1255 4.28061 10.2194C4.3745 10.3133 4.48615 10.3875 4.60905 10.4378C4.73195 10.4881 4.86363 10.5134 4.99641 10.5123C5.12919 10.5111 5.26041 10.4835 5.38241 10.4311C5.50441 10.3787 5.61476 10.3025 5.707 10.207L12 3.914V6.5C12 6.76522 12.1054 7.01957 12.2929 7.20711C12.4804 7.39464 12.7348 7.5 13 7.5C13.2652 7.5 13.5196 7.39464 13.7071 7.20711C13.8946 7.01957 14 6.76522 14 6.5V1.5C14 1.23478 13.8946 0.98043 13.7071 0.792893C13.5196 0.605357 13.2652 0.5 13 0.5H8Z" fill="#78716C"/>
      <path d="M2 2.5C1.46957 2.5 0.960859 2.71071 0.585786 3.08579C0.210714 3.46086 0 3.96957 0 4.5V12.5C0 13.0304 0.210714 13.5391 0.585786 13.9142C0.960859 14.2893 1.46957 14.5 2 14.5H10C10.5304 14.5 11.0391 14.2893 11.4142 13.9142C11.7893 13.5391 12 13.0304 12 12.5V9.5C12 9.23478 11.8946 8.98043 11.7071 8.79289C11.5196 8.60536 11.2652 8.5 11 8.5C10.7348 8.5 10.4804 8.60536 10.2929 8.79289C10.1054 8.98043 10 9.23478 10 9.5V12.5H2V4.5H5C5.26522 4.5 5.51957 4.39464 5.70711 4.20711C5.89464 4.01957 6 3.76522 6 3.5C6 3.23478 5.89464 2.98043 5.70711 2.79289C5.51957 2.60536 5.26522 2.5 5 2.5H2Z" fill="#78716C"/>
      <path d="M8 0.5C7.73479 0.5 7.48043 0.605357 7.2929 0.792893C7.10536 0.98043 7 1.23478 7 1.5C7 1.76522 7.10536 2.01957 7.2929 2.20711C7.48043 2.39464 7.73479 2.5 8 2.5H10.586L4.29301 8.793C4.19749 8.88525 4.12131 8.99559 4.0689 9.1176C4.01649 9.2396 3.98891 9.37082 3.98775 9.5036C3.9866 9.63638 4.0119 9.76806 4.06218 9.89095C4.11246 10.0139 4.18672 10.1255 4.28061 10.2194C4.3745 10.3133 4.48615 10.3875 4.60905 10.4378C4.73195 10.4881 4.86363 10.5134 4.99641 10.5123C5.12919 10.5111 5.26041 10.4835 5.38241 10.4311C5.50441 10.3787 5.61476 10.3025 5.707 10.207L12 3.914V6.5C12 6.76522 12.1054 7.01957 12.2929 7.20711C12.4804 7.39464 12.7348 7.5 13 7.5C13.2652 7.5 13.5196 7.39464 13.7071 7.20711C13.8946 7.01957 14 6.76522 14 6.5V1.5C14 1.23478 13.8946 0.98043 13.7071 0.792893C13.5196 0.605357 13.2652 0.5 13 0.5H8Z" fill="#78716C"/>
      <path d="M2 2.5C1.46957 2.5 0.960859 2.71071 0.585786 3.08579C0.210714 3.46086 0 3.96957 0 4.5V12.5C0 13.0304 0.210714 13.5391 0.585786 13.9142C0.960859 14.2893 1.46957 14.5 2 14.5H10C10.5304 14.5 11.0391 14.2893 11.4142 13.9142C11.7893 13.5391 12 13.0304 12 12.5V9.5C12 9.23478 11.8946 8.98043 11.7071 8.79289C11.5196 8.60536 11.2652 8.5 11 8.5C10.7348 8.5 10.4804 8.60536 10.2929 8.79289C10.1054 8.98043 10 9.23478 10 9.5V12.5H2V4.5H5C5.26522 4.5 5.51957 4.39464 5.70711 4.20711C5.89464 4.01957 6 3.76522 6 3.5C6 3.23478 5.89464 2.98043 5.70711 2.79289C5.51957 2.60536 5.26522 2.5 5 2.5H2Z" fill="#78716C"/>
    </svg>"""
      }
    )

}
