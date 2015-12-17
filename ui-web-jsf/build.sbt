import sbt._
import Keys._

val versions = Map[String, String](
  "akka"            -> "2.4.1",
  "apache-jsp"      -> "8.0.27",
  "cometd"          -> "3.0.7",
  "config"          -> "1.3.0",
  "eclipse-jdt"     -> "4.4.2",
  "jetty"           -> "9.3.6.v20151106",
  "jetty-jsp-jdt"   -> "2.3.3",
  "logback"         -> "1.1.3",
  "mojara"          -> "2.2.9",
  "primefaces"      -> "5.3",
  "scala"           -> "2.11.7",
  "slf4j"           -> "1.7.13",
  "stealthnet-core" -> "0.1.2-SNAPSHOT"
)


lazy val copyDependencies = TaskKey[Unit]("copy-dependencies")
lazy val distLocal = TaskKey[Unit]("dist-local")

def depFilename(name: String, scalaVersion: String) =
  name match {
    case "scala-library.jar" => s"scala-library-$scalaVersion.jar"
    case v => v
  }

val copyDepTask = copyDependencies <<= (baseDirectory, update, scalaVersion) map { (base, updateReport, scalaVersion) =>
  val dstBase = base / "target" / "lib"
  updateReport.select(configuration = Set("runtime")) foreach { srcPath =>
    val dstName = depFilename(srcPath.getName, scalaVersion)
    val dstPath = dstBase / dstName
    IO.copyFile(srcPath, dstPath, preserveLastModified = true)
  }
}

val distLocalTask = distLocal <<= (baseDirectory, update, scalaVersion, packageBin in Compile) map { (base, updateReport, scalaVersion, packagedFile) =>
  val dstBase = base / "target" / "dist"
  updateReport.select(configuration = Set("runtime")) foreach { srcPath =>
    val dstName = depFilename(srcPath.getName, scalaVersion)
    val dstPath = dstBase / "lib" / dstName
    IO.copyFile(srcPath, dstPath, preserveLastModified = true)
  }
  IO.copyDirectory(base / "src" / "main" / "webapp", dstBase / "webapp", overwrite = true, preserveLastModified = true)
  IO.copyDirectory(base / "src" / "main" / "config", dstBase / "config", overwrite = true, preserveLastModified = true)
  IO.copyDirectory(base / "src" / "main" / "scripts", dstBase, overwrite = true, preserveLastModified = true)
  // IO.copyXXX does not keep file permissions
  List("chmod", "+x", (dstBase / "launch.sh").getCanonicalPath).!
  IO.copyFile(packagedFile, dstBase / "webapp" / "WEB-INF" / "lib" / packagedFile.getName , preserveLastModified = true)
}


lazy val stealthnetUiWebJsf = project.in(file(".")).
  settings(
    organization := "stealthnet.scala",
    name := "stealthnet-ui-web-jsf",
    version := "0.1.2-SNAPSHOT",
    scalaVersion := versions("scala"),

    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-optimize",
      "-unchecked",
      "-Yinline-warnings"
    ),
    scalacOptions in (Compile, doc) ++= Seq("-diagrams", "-implicits"),

    libraryDependencies ++= Seq(
      "ch.qos.logback"                %  "logback-classic"                    % versions("logback") % "runtime",
      // Mojara implementation (https://javaserverfaces.java.net/download.html)
      // Note: could be replaced by MyFaces implementation
      "com.sun.faces"                 %  "jsf-api"                            % versions("mojara"),
      "com.sun.faces"                 %  "jsf-impl"                           % versions("mojara"),
      // Alternative to Mojara ?
      //"javax.faces", "javax.faces-api" % versions("mojara") % "provided",
      //"org.glassfish", "javax.faces" % versions("mojara") % "provided",
      "com.typesafe"                  %  "config"                             % versions("config"),
      "com.typesafe.akka"             %% "akka-actor"                         % versions("akka"),
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
      "org.eclipse.jetty.toolchain" % "jetty-jsp-jdt"                       % versions("jetty-jsp-jdt"),
      //"org.eclipse.jdt.core.compiler" %  "ecj"                                % versions("eclipse-jdt"),
      //"org.mortbay.jasper"            %  "apache-el"                          % versions("apache-jsp"),
      //"org.mortbay.jasper"            %  "apache-jsp"                         % versions("apache-jsp"),
      // Useful for CometD support (which otherwise relies on earlier versions).
      // org.eclipse.jetty/jetty-websocket in Jetty 8 moved to org.eclipse.jetty.websocket/websocket-server in Jetty 9
      //"org.eclipse.jetty" % "jetty-websocket" % versions("jetty"),
      // Jetty 9 supports JSR 356
      "org.eclipse.jetty.websocket"   % "javax-websocket-server-impl"         % versions("jetty"),
      //"org.eclipse.jetty" % "jetty-client" % versions("jetty"),
      "org.eclipse.jetty"             %  "jetty-jmx"                          % versions("jetty"),
      // jetty-util-ajax is separated from jetty-util in Jetty 9
      "org.eclipse.jetty"             %  "jetty-util-ajax"                    % versions("jetty"),
      "org.primefaces"                %  "primefaces"                         % versions("primefaces"),
      "org.scala-lang"                %  "scala-reflect"                      % versions("scala"),
      "org.slf4j"                     %  "jul-to-slf4j"                       % versions("slf4j"),
      "org.slf4j"                     %  "slf4j-api"                          % versions("slf4j"),
      "stealthnet.scala"              %% "stealthnet-core"                    % versions("stealthnet-core")
    ),

    resolvers += Resolver.mavenLocal,
    publishMavenStyle := true,
    publishTo := Some(Resolver.mavenLocal),

    copyDepTask,
    distLocalTask
  )
