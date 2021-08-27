package hackathon

import hackathon.api.{Issue, IssueAPI}
import zio.stream.UStream
import zio._
import zio.random.Random

case class IssueAPILive(random: zio.random.Random.Service) extends IssueAPI {
  override def issues: UStream[Issue] = UStream.empty
}

object IssueAPILive {
  val layer: URLayer[Random, Has[IssueAPI]] =
    (IssueAPILive.apply _).toLayer[IssueAPI]
}
