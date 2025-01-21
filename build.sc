package build

import mill._
import scalalib._

object parser extends ScalaModule {
  def scalaVersion = "3.3.4"
  def ivyDeps = Agg(
    ivy"org.scodec::scodec-core:2.3.2",
    ivy"com.lihaoyi::mainargs:0.6.2",
    ivy"org.typelevel::cats-core:2.13.0",
  )

  object test extends ScalaTests {
    def ivyDeps       = Agg(ivy"com.disneystreaming::weaver-cats:0.8.3")
    def testFramework = "weaver.framework.CatsEffect"
  }
}

object server extends ScalaModule {
  def scalaVersion: T[String] = "3.3.4"

  override def ivyDeps: T[Agg[Dep]] = Agg(
    ivy"net.sigusr::fs2-mqtt:1.0.1"
  )
}
