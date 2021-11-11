package hackathon.api

import zio._
import zio.stream._

trait IssueAPI {
  def issues: UIO[List[Issue]]

  def issueStream: UStream[Issue]

  def claim(owner: String, repo: String, issueNumber: Int, githubId: Long): Task[Unit]
}
