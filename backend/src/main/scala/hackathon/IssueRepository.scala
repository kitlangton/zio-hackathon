package hackathon

import hackathon.api.{Issue, IssueId}
import zio._
import zio.macros.accessible
import zio.magic._
import zio.stream.{UStream, ZStream}

import java.sql.Connection
import java.util.UUID

@accessible
trait IssueRepository {
  def all: Task[List[Issue]]

  def save(list: List[Issue]): Task[Unit]

  def claim(issueId: IssueId, user: String): Task[Unit]

  def issueStream: UStream[Issue]

  def claimsStream: UStream[Claim]

  def saveUser(login: String, id: Long, token: String): Task[Unit]

  def users: Task[List[String]]

  def accessToken(githubId: Long): Task[String]
}

object IssueRepository {
  val live: ZLayer[Has[Connection], Nothing, Has[IssueRepository]] = {
    for {
      connection <- ZIO.service[Connection]
      issueHub   <- Hub.unbounded[Issue]
      claimsHub  <- Hub.unbounded[Claim]
    } yield IssueRepositoryLive(connection, issueHub, claimsHub)
  }.toLayer[IssueRepository]
}

final case class Claim(issueId: IssueId, s: String)

final case class IssueRepositoryLive(
    connection: Connection,
    issueHub: Hub[Issue],
    claimsHub: Hub[Claim]
) extends IssueRepository {
  import QuillContext._

  val env = Has(connection)

  override def save(issues: List[Issue]): Task[Unit] =
    for {
      _ <- run {
        liftQuery(issues).foreach { i =>
          query[Issue]
            .insert(i)
            .onConflictUpdate(_.id)(_.title -> _.title, _.body -> _.body)
        }
      }.provide(env).unit
      _ <- issueHub.publishAll(issues)
    } yield ()

  override def claim(issueId: IssueId, user: String): Task[Unit] =
    for {
      _ <- run {
        query[Issue].filter(issue => issue.id == lift(issueId)).update(_.claimant -> Some(lift(user)))
      }.provide(env).unit
      issue <- run {
        query[Issue].filter { _.id == lift(issueId) }
      }.provide(env).map(_.head)
      _ <- issueHub.publish(issue)
    } yield ()

  override def all: Task[List[Issue]] =
    run(query[Issue]).provide(env)

  override def issueStream: UStream[Issue] =
    ZStream.fromHub(issueHub)

  override def claimsStream: UStream[Claim] =
    ZStream.fromHub(claimsHub)

  override def saveUser(login: String, id: Long, token: String): Task[Unit] = {
    val account = Account(id = UUID.randomUUID(), githubLogin = login, githubId = id, githubAccessToken = token)
    run {
      query[Account].insert(lift(account))
    }.provide(env).unit
  }

  override def users: Task[List[String]] =
    run { query[Account].map(_.githubLogin) }
      .provide(env)

  override def accessToken(id: Long): Task[String] =
    run { query[Account].filter(_.githubId == lift(id)).map(_.githubAccessToken) }
      .map(_.head)
      .provide(env)
}

final case class Account(
    id: UUID,
    githubLogin: String,
    githubId: Long,
    githubAccessToken: String
)

object IssueRepositoryDemo extends App {
  val exampleIssues = List(
    Issue(
      id = IssueId(1),
      number = 1,
      title = "FANCY ISSUE",
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

  // TODO: Test Containers
  // TODO: GitHub OAuth
  val program: ZIO[Has[IssueRepository], Throwable, Unit] =
    for {
      _      <- IssueRepository.save(exampleIssues)
      issues <- IssueRepository.all
      _      <- IssueRepository.claim(IssueId(2), "bobby")
      _      <- ZIO.debug(PrettyPrint(issues))
    } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    program.inject(IssueRepository.live, QuillContext.live).exitCode
}
