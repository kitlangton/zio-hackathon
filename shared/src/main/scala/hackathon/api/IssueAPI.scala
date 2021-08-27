package hackathon.api

import zio._
import zio.stream._

trait IssueAPI {
  def issues: UStream[Issue]
}
