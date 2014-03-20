import sbt._
import Keys._


object StealthNetBuild extends Build {

  lazy val copyDependencies = TaskKey[Unit]("copy-dependencies")
  lazy val copyPom = TaskKey[Unit]("copy-pom")

  def copyDepTask(base: File) = copyDependencies <<= (update, crossTarget, scalaVersion) map { (updateReport, _, scalaVersion) =>
    base.getParentFile.mkdirs
    updateReport.select(configuration = Set("runtime")) foreach { srcPath =>
      val dstName = srcPath.getName match {
        case "scala-library.jar" => s"scala-library-$scalaVersion.jar"
        case v => v
      }
      val dstPath = base / "target" / "lib" / dstName
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

