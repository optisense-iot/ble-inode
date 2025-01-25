package build

import mill._
import mill.contrib.docker.DockerModule
import scalalib._

import $ivy.`com.lihaoyi::mill-contrib-docker:`

object inodeParser extends ScalaModule {
  override def scalaVersion = "3.3.4"
  override def ivyDeps = Agg(
    ivy"org.scodec::scodec-core:2.3.2",
    ivy"com.lihaoyi::mainargs:0.6.2",
    ivy"org.typelevel::cats-core:2.13.0",
  )

  object test extends ScalaTests {
    override def ivyDeps = Agg(
      ivy"com.disneystreaming::weaver-cats:0.8.3"
    )
    override def testFramework = "weaver.framework.CatsEffect"
  }
}

object server extends ScalaModule with DockerModule {
  override def moduleDeps: Seq[JavaModule] = Seq(inodeParser)

  override def scalaVersion: T[String] = "3.3.4"

  override def ivyDeps: T[Agg[Dep]] = Agg(
    ivy"net.sigusr::fs2-mqtt:1.0.1",
    ivy"com.monovore::decline-effect:2.5.0",
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core::${Versions.jsoniter}",
    ivy"io.circe::circe-parser:0.14.10",
    ivy"io.circe::circe-generic:0.14.10",
  )

  override def compileIvyDeps: T[Agg[Dep]] = Agg(
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros::${Versions.jsoniter}"
  )

  object docker extends DockerConfig

  object test extends ScalaTests {
    override def ivyDeps = Agg(
      ivy"com.disneystreaming::weaver-cats:0.8.3"
    )
    override def testFramework = "weaver.framework.CatsEffect"
  }
}

object Versions {
  val jsoniter = "2.33.0"
}
