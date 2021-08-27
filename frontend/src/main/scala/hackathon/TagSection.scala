package hackathon

import com.raquo.laminar.api.L._

final case class TagSection(title: String, tags: List[String]) extends Component {
  val selectedTags =
    Var(Set.empty[String])

  private val $anyTagSelected: Signal[Boolean] =
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
      marginBottom("24px"),
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
