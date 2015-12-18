import sbt._
import sbt.Keys._

lazy val versions = Map[String, String](
  "akka"             -> "2.4.1",
  //"apache-jsp"       -> "8.0.27",
  "bouncycastle"     -> "1.53",
  "cometd"           -> "3.0.7",
  "config"           -> "1.3.0",
  //"eclipse-jdt"      -> "4.4.2",
  "jetty"            -> "9.3.6.v20151106",
  "jetty-jsp-jdt"    -> "2.3.3",
  "junit"            -> "4.12",
  "logback"          -> "1.1.3",
  "mojara"           -> "2.2.9",
  "netty"            -> "4.0.33.Final",
  "primefaces"       -> "5.3",
  "scala"            -> "2.11.7",
  "scala-xml"        -> "1.0.5",
  "scalatest"        -> "2.2.4",
  "suiryc-scala"     -> "0.0.2-SNAPSHOT",
  "slf4j"            -> "1.7.13",
  "stealthnet-scala" -> "0.1.2-SNAPSHOT"
)


lazy val packagedDependencies = taskKey[Seq[File]]("Project packaged dependencies")
lazy val copyDependencies = taskKey[Unit]("copy-dependencies")
lazy val distLocal = taskKey[Unit]("dist-local")

// Note: we need a taskDyn since we don't work with a 'static' project reference
val packagedDependenciesImpl = Def.taskDyn {
  // We want to work with the current project dependencies, so create a scope
  // filter including those.
  val projects = thisProject.value.dependencies.map(_.project)
  val filter = ScopeFilter(inProjects(projects:_*))

  // Note: other way to create this filter:
  //val ref = thisProjectRef.value
  //val filter = ScopeFilter(inDependencies(ref) -- inProjects(ref))

  // The actual task getting the package of a given dependency
  val packageTask = Def.task {
    (packageBin in Compile).value
  }

  // Filter the task on the wanted dependencies
  packageTask.all(filter)
}

// Filter dependency filename
def dependencyFilename(name: String, scalaVersion: String) =
  name match {
    case "scala-library.jar" => s"scala-library-$scalaVersion.jar"
    case v => v
  }

def copyDependenciesImpl(base: File, dstRelative: File, updateReport: UpdateReport, packaged: Seq[File], scalaVersion: String): Unit = {
  val dstBase = base / dstRelative.getPath
  (updateReport.select(configuration = Set("runtime")) ++ packaged).foreach { srcPath =>
    val dstName = dependencyFilename(srcPath.getName, scalaVersion)
    val dstPath = dstBase / dstName
    IO.copyFile(srcPath, dstPath, preserveLastModified = true)
  }
}

def copyDependenciesTask(dstRelative: File = file("target") / "lib") =
  copyDependencies <<= (baseDirectory, update, packagedDependencies, scalaVersion).map {
    (base, updateReport, packaged, scalaVersion) =>

      copyDependenciesImpl(base, dstRelative, updateReport, packaged, scalaVersion)
  }

lazy val distLocalPath = file("target") / "dist"
lazy val distLocalTask =
  distLocal <<= (baseDirectory, update, packagedDependencies, scalaVersion, packageBin in Compile).map {
    (base, updateReport, packaged, scalaVersion, packagedFile) =>

      copyDependenciesImpl(base, distLocalPath / "lib", updateReport, packaged, scalaVersion)
      val dstBase = base / distLocalPath.getPath
      IO.copyDirectory(base / "src" / "main" / "webapp", dstBase / "webapp", overwrite = true, preserveLastModified = true)
      IO.copyDirectory(base / "src" / "main" / "config", dstBase / "config", overwrite = true, preserveLastModified = true)
      IO.copyDirectory(base / "src" / "main" / "scripts", dstBase, overwrite = true, preserveLastModified = true)
      // IO.copyXXX does not keep file permissions
      List("chmod", "+x", (dstBase / "launch.sh").getCanonicalPath).!
      IO.copyFile(packagedFile, dstBase / "webapp" / "WEB-INF" / "lib" / packagedFile.getName , preserveLastModified = true)
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

  libraryDependencies ++= Seq(
    "ch.qos.logback"         %  "logback-classic"   % versions("logback")      % "runtime",
    "com.typesafe"           %  "config"            % versions("config"),
    "com.typesafe.akka"      %% "akka-actor"        % versions("akka"),
    "org.slf4j"              %  "slf4j-api"         % versions("slf4j"),
    "suiryc"                 %% "suiryc-scala-core" % versions("suiryc-scala")
  ),

  publishMavenStyle := true,
  publishTo := Some(Resolver.mavenLocal),

  packagedDependencies := packagedDependenciesImpl.value,
  copyDependenciesTask()
)


lazy val core = project.in(file("core")).
  settings(commonSettings:_*).
  settings(
    name := "stealthnet-core",

    libraryDependencies ++= Seq(
      "com.typesafe.akka"      %% "akka-testkit"      % versions("akka")         % "test",
      "io.netty"               %  "netty-all"         % versions("netty"),
      "junit"                  %  "junit"             % versions("junit")        % "test",
      "org.bouncycastle"       %  "bcprov-jdk15on"    % versions("bouncycastle"),
      "org.scala-lang.modules" %% "scala-xml"         % versions("scala-xml"),
      "org.scalatest"          %% "scalatest"         % versions("scalatest")    % "test"
    )
  )

lazy val uiWebJsf = project.in(file("ui-web-jsf")).
  dependsOn(core).
  settings(commonSettings:_*).
  settings(
    name := "stealthnet-ui-web-jsf",

    libraryDependencies ++= Seq(
      // Mojara implementation (https://javaserverfaces.java.net/download.html)
      // Note: could be replaced by MyFaces implementation
      "com.sun.faces"                 %  "jsf-api"                            % versions("mojara"),
      "com.sun.faces"                 %  "jsf-impl"                           % versions("mojara"),
      // Alternative to Mojara ?
      //"javax.faces", "javax.faces-api" % versions("mojara") % "provided",
      //"org.glassfish", "javax.faces" % versions("mojara") % "provided",
      "org.cometd.java"               %  "bayeux-api"                         % versions("cometd"),
      "org.cometd.java"               %  "cometd-java-server"                 % versions("cometd"),
      // cometd-websocket-jetty in CometD 2.x moved to cometd-java-websocket-jetty-server in CometD 3.x
      // cometd-java-websocket-javax-server supports JSR 356 in CometD 3.x
      "org.cometd.java"               %  "cometd-java-websocket-javax-server" % versions("cometd"),
      // org.eclipse.jetty/jetty-jsp in Jetty <= 9.2 replaced by org.eclipse.jetty/apache-jsp since 9.3
      //"org.eclipse.jetty"           %  "jetty-jsp"                          % versions("jetty"),
      "org.eclipse.jetty"             %  "apache-jsp"                         % versions("jetty"),
      "org.eclipse.jetty"             %  "jetty-webapp"                       % versions("jetty"),
      "org.eclipse.jetty"             %  "jetty-server"                       % versions("jetty"),
      // JSP compiler necessary with Jetty 9; previously provided by org.apache.jasper.glassfish through jetty-jsp
      // org.eclipse.jetty.toolchain/jetty-jsp-jdt is available but not upgraded anymore ?
      // Jetty distribution comes with Eclipse JDT compiler and Apache EL, but it does not work if replacing jetty-jsp-jdt ...
      "org.eclipse.jetty.toolchain"   % "jetty-jsp-jdt"                       % versions("jetty-jsp-jdt"),
      //"org.eclipse.jdt.core.compiler" %  "ecj"                                % versions("eclipse-jdt"),
      //"org.mortbay.jasper"            %  "apache-el"                          % versions("apache-jsp"),
      //"org.mortbay.jasper"            %  "apache-jsp"                         % versions("apache-jsp"),
      // Useful for CometD support (which otherwise relies on earlier versions).
      // org.eclipse.jetty/jetty-websocket in Jetty 8 moved to org.eclipse.jetty.websocket/websocket-server in Jetty 9
      //"org.eclipse.jetty" % "jetty-websocket" % versions("jetty"),
      // Jetty 9 supports JSR 356
      "org.eclipse.jetty.websocket"   %  "javax-websocket-server-impl"        % versions("jetty"),
      //"org.eclipse.jetty" % "jetty-client" % versions("jetty"),
      "org.eclipse.jetty"             %  "jetty-jmx"                          % versions("jetty"),
      // jetty-util-ajax is separated from jetty-util in Jetty 9
      "org.eclipse.jetty"             %  "jetty-util-ajax"                    % versions("jetty"),
      "org.primefaces"                %  "primefaces"                         % versions("primefaces"),
      "org.scala-lang"                %  "scala-reflect"                      % versions("scala"),
      "org.slf4j"                     %  "jul-to-slf4j"                       % versions("slf4j")
    ),

    distLocalTask
  )


// Notes: 'aggregate' can be used so that commands used on 'root' project can be executed in each subproject
// 'dependsOn' can be used so that an assembly jar can be built with all subprojects resources (and dependencies)
lazy val root = project.in(file(".")).
  aggregate(core, uiWebJsf).
  settings(commonSettings:_*).
  settings(
    name := "stealthnet-scala",
    libraryDependencies := Seq.empty,
    publishArtifact in Compile := false
  )
