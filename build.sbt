import sbt._
import sbt.Keys._

lazy val versions = Map[String, String](
  "akka"             -> "2.4.1",
  "bouncycastle"     -> "1.53",
  "config"           -> "1.3.0",
  "junit"            -> "4.12",
  "logback"          -> "1.1.3",
  "netty"            -> "4.0.33.Final",
  "scala"            -> "2.11.7",
  "scala-xml"        -> "1.0.5",
  "scalatest"        -> "2.2.4",
  "stealthnet-scala" -> "0.1.2-SNAPSHOT",
  "suiryc-scala"     -> "0.0.2-SNAPSHOT",
  "slf4j"            -> "1.7.13"
)


lazy val copyDependencies = TaskKey[Unit]("copy-dependencies")

def depFilename(name: String, scalaVersion: String) =
  name match {
    case "scala-library.jar" => s"scala-library-$scalaVersion.jar"
    case v => v
  }

lazy val copyDepTask = copyDependencies <<= (baseDirectory, update, scalaVersion) map { (base, updateReport, scalaVersion) =>
  val dstBase = base / "target" / "lib"
  updateReport.select(configuration = Set("runtime")) foreach { srcPath =>
    val dstName = depFilename(srcPath.getName, scalaVersion)
    val dstPath = dstBase / dstName
    IO.copyFile(srcPath, dstPath, preserveLastModified = true)
  }
}

lazy val commonSettings = Seq(
  organization := "stealthnet.scala",
  version := versions("stealthnet-scala"),
  scalaVersion := versions("scala"),

  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-optimize",
    "-unchecked",
    "-Yinline-warnings"
  ),
  scalacOptions in (Compile, doc) ++= Seq("-diagrams", "-implicits"),
  resolvers += Resolver.mavenLocal,
  publishMavenStyle := true,
  publishTo := Some(Resolver.mavenLocal),

  copyDepTask
)


lazy val core = project.in(file("core")).
  settings(commonSettings:_*).
  settings(
    name := "stealthnet-core",

    libraryDependencies ++= Seq(
      "ch.qos.logback"         %  "logback-classic"   % versions("logback")      % "runtime",
      "com.typesafe"           %  "config"            % versions("config"),
      "com.typesafe.akka"      %% "akka-actor"        % versions("akka"),
      "com.typesafe.akka"      %% "akka-testkit"      % versions("akka")         % "test",
      "io.netty"               %  "netty-all"         % versions("netty"),
      "junit"                  %  "junit"             % versions("junit")        % "test",
      "org.bouncycastle"       %  "bcprov-jdk15on"    % versions("bouncycastle"),
      "org.scala-lang.modules" %% "scala-xml"         % versions("scala-xml"),
      "org.scalatest"          %% "scalatest"         % versions("scalatest")    % "test",
      "org.slf4j"              %  "slf4j-api"         % versions("slf4j"),
      "suiryc"                 %% "suiryc-scala-core" % versions("suiryc-scala")
    )
  )

// Notes: 'aggregate' can be used so that commands used on 'root' project can be executed in each subproject
// 'dependsOn' can be used so that an assembly jar can be built with all subprojects resources (and dependencies)
lazy val root = project.in(file(".")).
  aggregate(core).
  settings(commonSettings:_*).
  settings(
    name := "stealthnet-scala",
    libraryDependencies := Seq.empty,
    publishArtifact in Compile := false
  )
