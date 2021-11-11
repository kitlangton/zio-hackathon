package hackathon

import zio._
import zio.config._
import zio.config.magnolia._
import zio.config.typesafe._

final case class GitHubConfig(accessToken: String, clientId: String, clientSecret: String)

final case class Config(github: GitHubConfig)

object Config {
  val descriptor: ConfigDescriptor[Config] =
    DeriveConfigDescriptor.descriptor[Config]

  val live: ULayer[Has[GitHubConfig]] =
    TypesafeConfig
      .fromDefaultLoader(descriptor)
      .orDie
      .project(_.github)

  val service: URIO[Has[Config], Config] = ZIO.service[Config]
}
