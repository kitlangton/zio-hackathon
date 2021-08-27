package hackathon.protocol

import zio._

trait ExampleService {
  def magicNumber: UIO[Int]
}
