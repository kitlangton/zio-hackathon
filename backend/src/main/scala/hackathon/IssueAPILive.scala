package hackathon

import github.GitHubAPI
import hackathon.api.{Issue, IssueAPI}
import zio.stream._
import zio._
import zio.random.Random

case class IssueAPILive(gitHubAPI: GitHubAPI, issueRepository: IssueRepository, random: zio.random.Random.Service)
    extends IssueAPI {
  override def issues: UIO[List[Issue]] =
    issueRepository.all.orDie

  override def issueStream: UStream[Issue] =
    issueRepository.issueStream

  override def claim(owner: String, repo: String, issueNumber: Int, githubId: Long): Task[Unit] =
    for {
      token <- issueRepository.accessToken(githubId)
      _     <- gitHubAPI.comment(token, owner, repo, issueNumber, "I will look into this :sunglasses:")
    } yield ()
}

object IssueAPILive {
  val layer: URLayer[Has[GitHubAPI] with Has[IssueRepository] with Has[Random.Service], Has[IssueAPI]] =
    (IssueAPILive.apply _).toLayer[IssueAPI]
}
