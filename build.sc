package build

import com.goyeau.mill.git.GitVersionedPublishModule
import mill._
import mill.contrib.docker.DockerModule
import mill.scalalib._
import mill.scalalib.publish._

import $ivy.`com.goyeau::mill-git::0.2.7`
import $ivy.`com.lihaoyi::mill-contrib-docker:`

object inodeParser extends ScalaModule with GitVersionedPublishModule with PublishModule {
  override def scalaVersion = "3.3.4"
  override def ivyDeps = Agg(
    ivy"org.scodec::scodec-core:2.3.2",
    ivy"com.lihaoyi::mainargs:0.6.2",
    ivy"org.typelevel::cats-core:2.13.0",
  )

  def artifactId = "inode-parser"

  def pomSettings = PomSettings(
    description = "BLE INode parser lib",
    organization = "iot.optisense",
    url = "https://github.com/optisense-iot/ble-inode",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github("optisense-iot", "ble-inode"),
    developers = Seq(Developer("ghostbuster91", "Kasper Kondzielski", "https://github.com/ghostbuster91")),
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
    ivy"com.softwaremill.sttp.client4::cats:4.0.0-M25",
    ivy"com.softwaremill.sttp.client4::jsoniter:4.0.0-M25",
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
