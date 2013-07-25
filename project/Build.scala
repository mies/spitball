import sbt._
import Keys._
import spray.revolver.RevolverPlugin._
import com.typesafe.sbt.SbtStartScript

object ApplicationBuild extends Build {

  val appName = "spitball"
  val appVersion = "1.0-SNAPSHOT"
  val sprayVersion = "1.1-M7"
  val appDependencies = Seq(
    "io.spray" % "spray-can" % sprayVersion,
    "io.spray" % "spray-routing" % sprayVersion,
    "io.spray" %% "spray-json" % "1.2.3",
    "redis.clients" % "jedis" % "2.1.0",
    "com.typesafe.akka" %% "akka-actor" % "2.1.1",
    "com.typesafe.akka" %% "akka-slf4j" % "2.1.1" % "runtime",
    "ch.qos.logback" % "logback-classic" % "1.0.9" % "compile",
    "io.spray" % "spray-testkit" % sprayVersion % "test",
    "org.specs2" %% "specs2" % "1.14" % "test",
    "org.mockito" % "mockito-all" % "1.9.0" % "test"
  )

  val main = Project(appName, file("."))
    .settings(scalaVersion := "2.10.1")
    .settings(libraryDependencies ++= appDependencies)
    .settings(resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
    .settings(resolvers += "Spray Repo" at "http://repo.spray.io")
    .settings(Revolver.settings: _*)
    .settings(SbtStartScript.startScriptForClassesSettings: _*)
}
