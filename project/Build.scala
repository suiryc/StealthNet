import sbt._
import Keys._


object StealthNetBuild extends Build {

  lazy val copyDependencies = TaskKey[Unit]("copy-dependencies")
  lazy val copyPom = TaskKey[Unit]("copy-pom")

  def depFilename(name: String, scalaVersion: String) =
    name match {
      case "scala-library.jar" => s"scala-library-$scalaVersion.jar"
      case v => v
    }

  def copyDepTask(base: File) = copyDependencies <<= (update, scalaVersion) map { (updateReport, scalaVersion) =>
    val dstBase = base / "target" / "lib"
    updateReport.select(configuration = Set("runtime")) foreach { srcPath =>
      val dstName = depFilename(srcPath.getName, scalaVersion)
      val dstPath = dstBase / dstName
      IO.copyFile(srcPath, dstPath, preserveLastModified = true)
    }
  }

  def copyPomTask(base: File) = copyPom <<= makePom map { pom =>
    IO.copyFile(pom, base / "pom.xml")
  }

  lazy val base = file(".").getCanonicalFile

  lazy val root = Project(
    "root",
    base,
    settings = Defaults.defaultSettings ++ Seq(
      copyDepTask(base), copyPomTask(base)
    )
  )
}

