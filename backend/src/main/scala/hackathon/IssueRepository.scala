package hackathon

import hackathon.api.{Issue, IssueId}
import zio._
import zio.magic._
import zio.macros.accessible

import java.sql.Connection

@accessible
trait IssueRepository {
  def all: Task[List[Issue]]

  def save(list: List[Issue]): Task[Unit]

  def claim(issueId: IssueId, user: String): Task[Unit]
}

object IssueRepository {
  val live: URLayer[Has[Connection], Has[IssueRepository]] =
    IssueRepositoryLive.toLayer[IssueRepository]
}

final case class IssueRepositoryLive(connection: Connection) extends IssueRepository {
  import QuillContext._

  val env = Has(connection)

  override def save(list: List[Issue]): Task[Unit] =
    run {
      liftQuery(list).foreach { i =>
        query[Issue]
          .insert(i)
          .onConflictUpdate(_.id)(_.title -> _.title, _.body -> _.body)
      }
    }.provide(env).unit

  override def claim(issueId: IssueId, user: String): Task[Unit] =
    run {
      query[Issue].filter(_.id == lift(issueId)).update(_.claimant -> Some(lift(user)))
    }.provide(env).unit

  override def all: Task[List[Issue]] =
    run(query[Issue]).provide(env)
}

object IssueRepositoryDemo extends App {
  val exampleIssues = List(
    Issue(
      id = IssueId(1),
      number = 1,
      title = "Quill-jdbc-zio causes thread starvation",
      body = Some("Cool issue."),
      owner = "kitlangton",
      repo = "zio-app",
      url = "github.com",
      claimant = Some("adamfraser")
    ),
    Issue(
      id = IssueId(2),
      number = 2,
      title = "Another Issue",
      body = None,
      owner = "zio",
      repo = "zio-cli",
      url = "github.com",
      claimant = None
    )
  )

  val program = for {
    _      <- IssueRepository.save(exampleIssues)
    issues <- IssueRepository.all
    _      <- IssueRepository.claim(IssueId(2), "bobby")
    _      <- ZIO.debug(PrettyPrint(issues))
  } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    program
      .inject(
        IssueRepository.live,
        QuillContext.live
      )
      .exitCode
}
