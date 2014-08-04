import sbt._
import Keys._


object StealthNetBuild extends Build {

  lazy val base = file(".").getCanonicalFile

  lazy val copyDependencies = TaskKey[Unit]("copy-dependencies")
  lazy val copyPom = TaskKey[Unit]("copy-pom")

  def depFilename(name: String, scalaVersion: String) =
    name match {
      case "scala-library.jar" => s"scala-library-$scalaVersion.jar"
      case v => v
    }

  val copyDepTask = copyDependencies <<= (update, scalaVersion) map { (updateReport, scalaVersion) =>
    val dstBase = base / "target" / "lib"
    updateReport.select(configuration = Set("runtime")) foreach { srcPath =>
      val dstName = depFilename(srcPath.getName, scalaVersion)
      val dstPath = dstBase / dstName
      IO.copyFile(srcPath, dstPath, preserveLastModified = true)
    }
  }

  val copyPomTask = copyPom <<= (makePom, streams) map { (pom, s) =>
    val dest = base / "pom.xml"
    s.log.info(s"Copy pom: $dest")
    IO.copyFile(pom, dest)
  }

  val extCompile = compile <<= (compile in Compile) dependsOn(copyPom)

  lazy val root = Project(
    id = "stealthnet",
    base = base,
    settings = Defaults.defaultSettings ++ Seq(
      copyDepTask, copyPomTask, extCompile
    )
  )
}

