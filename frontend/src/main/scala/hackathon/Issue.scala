package hackathon

import java.util.UUID

final case class Issue(
    id: UUID,
    title: String,
    project: String,
    number: Int,
    claimant: Option[String]
)

object Issue {
  def apply(
      title: String,
      project: String,
      number: Int,
      claimant: Option[String] = None
  ): Issue =
    Issue(UUID.randomUUID(), title, project, number, claimant)

  val examples = List(
    Issue(
      "ZIO 2.0: Use consistent naming convention in Gen.*",
      "ZIO",
      5441,
      Some("kitlangton")
    ),
    Issue(
      "Use javascript weak references",
      "ZIO",
      5442
    ),
    Issue(
      "Logging middleware and document creation of middlewares",
      "ZIO-HTTP",
      339,
      Some("adamfraser")
    ),
    Issue(
      "Document usage of ZIO Http Test API",
      "ZIO-HTTP",
      333
    ),
    Issue(
      "Share event loop betwen client and server",
      "ZIO-HTTP",
      307
    ),
    Issue(
      "Add support for enumeratum",
      "CALIBAN",
      623
    ),
    Issue(
      "ZIO 2.0: Use consistent naming convention in Gen.*",
      "ZIO",
      5441,
      Some("kitlangton")
    ),
    Issue(
      "Use javascript weak references",
      "ZIO",
      5442
    ),
    Issue(
      "Logging middleware and document creation of middlewares",
      "ZIO-HTTP",
      339,
      Some("adamfraser")
    ),
    Issue(
      "Document usage of ZIO Http Test API",
      "ZIO-HTTP",
      333
    ),
    Issue(
      "Share event loop betwen client and server",
      "ZIO-HTTP",
      307
    ),
    Issue(
      "Add support for enumeratum",
      "CALIBAN",
      623
    ),
    Issue(
      "ZIO 2.0: Use consistent naming convention in Gen.*",
      "ZIO",
      5441,
      Some("kitlangton")
    ),
    Issue(
      "Use javascript weak references",
      "ZIO",
      5442
    ),
    Issue(
      "Logging middleware and document creation of middlewares",
      "ZIO-HTTP",
      339,
      Some("adamfraser")
    ),
    Issue(
      "Document usage of ZIO Http Test API",
      "ZIO-HTTP",
      333
    ),
    Issue(
      "Share event loop betwen client and server",
      "ZIO-HTTP",
      307
    ),
    Issue(
      "Add support for enumeratum",
      "CALIBAN",
      623
    ),
    Issue(
      "ZIO 2.0: Use consistent naming convention in Gen.*",
      "ZIO",
      5441,
      Some("kitlangton")
    ),
    Issue(
      "Use javascript weak references",
      "ZIO",
      5442
    ),
    Issue(
      "Logging middleware and document creation of middlewares",
      "ZIO-HTTP",
      339,
      Some("adamfraser")
    ),
    Issue(
      "Document usage of ZIO Http Test API",
      "ZIO-HTTP",
      333
    ),
    Issue(
      "Share event loop betwen client and server",
      "ZIO-HTTP",
      307
    ),
    Issue(
      "Add support for enumeratum",
      "CALIBAN",
      623
    ),
    Issue(
      "ZIO 2.0: Use consistent naming convention in Gen.*",
      "ZIO",
      5441,
      Some("kitlangton")
    ),
    Issue(
      "Use javascript weak references",
      "ZIO",
      5442
    ),
    Issue(
      "Logging middleware and document creation of middlewares",
      "ZIO-HTTP",
      339,
      Some("adamfraser")
    ),
    Issue(
      "Document usage of ZIO Http Test API",
      "ZIO-HTTP",
      333
    ),
    Issue(
      "Share event loop betwen client and server",
      "ZIO-HTTP",
      307
    ),
    Issue(
      "Add support for enumeratum",
      "CALIBAN",
      623
    ),
    Issue(
      "ZIO 2.0: Use consistent naming convention in Gen.*",
      "ZIO",
      5441,
      Some("kitlangton")
    ),
    Issue(
      "Use javascript weak references",
      "ZIO",
      5442
    ),
    Issue(
      "Logging middleware and document creation of middlewares",
      "ZIO-HTTP",
      339,
      Some("adamfraser")
    ),
    Issue(
      "Document usage of ZIO Http Test API",
      "ZIO-HTTP",
      333
    ),
    Issue(
      "Share event loop betwen client and server",
      "ZIO-HTTP",
      307
    ),
    Issue(
      "Add support for enumeratum",
      "CALIBAN",
      623
    ),
    Issue(
      "ZIO 2.0: Use consistent naming convention in Gen.*",
      "ZIO",
      5441,
      Some("kitlangton")
    ),
    Issue(
      "Use javascript weak references",
      "ZIO",
      5442
    ),
    Issue(
      "Logging middleware and document creation of middlewares",
      "ZIO-HTTP",
      339,
      Some("adamfraser")
    ),
    Issue(
      "Document usage of ZIO Http Test API",
      "ZIO-HTTP",
      333
    ),
    Issue(
      "Share event loop betwen client and server",
      "ZIO-HTTP",
      307
    ),
    Issue(
      "Add support for enumeratum",
      "CALIBAN",
      623
    )
  )
}
