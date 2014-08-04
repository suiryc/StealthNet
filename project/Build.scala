import sbt._
import Keys._


object StealthNetBuild extends Build {

  lazy val base = file(".").getCanonicalFile

  lazy val copyDependencies = TaskKey[Unit]("copy-dependencies")
  lazy val copyPom = TaskKey[Unit]("copy-pom")
  lazy val distLocal = TaskKey[Unit]("dist-local")

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

  val distLocalTask = distLocal <<= (update, scalaVersion, packageBin in Compile) map { (updateReport, scalaVersion, packagedFile) =>
    val dstBase = base / "target" / "dist"
    updateReport.select(configuration = Set("runtime")) foreach { srcPath =>
      val dstName = depFilename(srcPath.getName, scalaVersion)
      val dstPath = dstBase / "lib" / dstName
      IO.copyFile(srcPath, dstPath, preserveLastModified = true)
    }
    IO.copyDirectory(base / "src" / "main" / "webapp", dstBase / "webapp", overwrite = true, preserveLastModified = true)
    IO.copyDirectory(base / "src" / "main" / "config", dstBase / "config", overwrite = true, preserveLastModified = true)
    IO.copyDirectory(base / "src" / "main" / "scripts", dstBase, overwrite = true, preserveLastModified = true)
    /* IO.copyXXX does not keep file permissions */
    List("chmod", "+x", (dstBase / "launch.sh").getCanonicalPath).!
    IO.copyFile(packagedFile, dstBase / "webapp" / "WEB-INF" / "lib" / packagedFile.getName , preserveLastModified = true)
  }

  lazy val root = Project(
    id = "stealthnet-ui-web-jsf",
    base = base,
    settings = Defaults.defaultSettings ++ Seq(
      copyDepTask, copyPomTask, distLocalTask, extCompile
    )
  )
}

