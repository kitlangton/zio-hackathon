package github

import hackathon.{Config, GitHubConfig, IssueRepository, PrettyPrint, QuillContext}
import sttp.client3._
import sttp.client3.httpclient.zio.{HttpClientZioBackend, SttpClient}
import zio._
import zio.magic._
import zio.json._
import zio.macros.accessible

@accessible
trait GitHubAPI {
  def user(accessToken: String): Task[GithubUser]

  def issues(owner: String, repo: String): Task[List[IssuePayload]]

  def comment(accessToken: String, owner: String, repo: String, issueNumber: Int, text: String): Task[Unit]
}

object GitHubAPI {
  val live: URLayer[Has[GitHubConfig] with Has[SttpClient.Service], Has[GitHubAPI]] =
    GitHubAPILive.toLayer[GitHubAPI]
}

final case class GitHubAPILive(config: GitHubConfig, sttp: SttpClient.Service) extends GitHubAPI {
  val baseUrl = uri"https://api.github.com/"

  def issueCommentUrl(owner: String, repo: String, issueNumber: Int) =
    uri"$baseUrl/repos/$owner/$repo/issues/$issueNumber/comments"

  def issuesUrl(owner: String, repo: String) =
    uri"$baseUrl/repos/$owner/$repo/issues"

  def issuesUrl(owner: String, repo: String, labels: String*) =
    uri"$baseUrl/repos/$owner/$repo/issues?labels=${labels.mkString(",")}"

  override def issues(owner: String, repo: String): Task[List[IssuePayload]] = {
    val request =
      basicRequest
        .get(issuesUrl(owner, repo, "zio-hackathon"))
        .header("Authorization", s"token ${config.accessToken}")

    sttp
      .send(request)
      .flatMap {
        _.body match {
          case Right(body) =>
            ZIO
              .fromEither(body.fromJson[List[IssuePayload]])
              .mapError(e => new Error(e))
          case Left(error) =>
            ZIO.fail(new Error(error))
        }

      }
  }

  override def comment(accessToken: String, owner: String, repo: String, issueNumber: Int, text: String): Task[Unit] = {
    val request =
      basicRequest
        .post(issueCommentUrl(owner, repo, issueNumber))
        .body(PostCommentBody(text).toJson)
        .header("Authorization", s"token $accessToken")

    sttp
      .send(request)
      .map(resp => ZIO.fromEither(resp.body).mapError(e => new Error(e)))
      .unit
  }

  override def user(accessToken: String): Task[GithubUser] =
    sttp
      .send(
        quickRequest
          .get(uri"$baseUrl/user")
          .header("Authorization", s"token $accessToken")
      )
      .flatMap { resp =>
        ZIO.fromEither(resp.body.fromJson[GithubUser]).mapError { e => new Error(s"Failed to parse user $e $resp") }
      }
}

object TestGitHubAPI extends App {
  val getIssues =
    GitHubAPI
      .issues("kitlangton", "zio-app")
      .tap { issues => ZIO.debug(PrettyPrint(issues)) }
      .inject(Config.live, HttpClientZioBackend.layer().orDie, GitHubAPI.live)
      .exitCode

  val postComment = GitHubAPI
    .comment("abcd", "kitlangton", "zio-hackathon", 7, "I will look into this :sunglasses:")
    .inject(Config.live, HttpClientZioBackend.layer().orDie, GitHubAPI.live)
    .exitCode

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    IssueRepository.users
      .debug("USERS")
      .inject(IssueRepository.live, QuillContext.live)
      .exitCode
}

final case class PostCommentBody(body: String)

object PostCommentBody {
  implicit val encoder: JsonEncoder[PostCommentBody] = DeriveJsonEncoder.gen[PostCommentBody]
}
