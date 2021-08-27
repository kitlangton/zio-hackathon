package github

import hackathon.{Config, GitHubConfig, PrettyPrint}
import sttp.client3._
import sttp.client3.httpclient.zio.{HttpClientZioBackend, SttpClient}
import zio._
import zio.magic._
import zio.json._
import zio.macros.accessible

@accessible
trait GitHubAPI {
  def getIssues(owner: String, repo: String): Task[List[IssuePayload]]
}

object GitHubAPI {
  val live: URLayer[Has[GitHubConfig] with Has[SttpClient.Service], Has[GitHubAPI]] =
    GitHubAPILive.toLayer[GitHubAPI]
}

final case class GitHubAPILive(config: GitHubConfig, sttp: SttpClient.Service) extends GitHubAPI {
  val baseUrl = uri"https://api.github.com/"

  def issuesUrl(owner: String, repo: String) =
    uri"$baseUrl/repos/$owner/$repo/issues"

  def issuesUrl(owner: String, repo: String, labels: String*) =
    uri"$baseUrl/repos/$owner/$repo/issues?labels=${labels.mkString(",")}"

  override def getIssues(owner: String, repo: String): Task[List[IssuePayload]] = {
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
            println(error)
            ZIO.fail(new Error(error))
        }

      }
  }
}

object TestGitHubAPI extends App {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    GitHubAPI
      .getIssues("kitlangton", "zio-app")
      .tap { is => ZIO.debug(PrettyPrint(is)) }
      .inject(Config.live, HttpClientZioBackend.layer().orDie, GitHubAPI.live)
      .exitCode
}
