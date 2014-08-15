organization := "stealthnet.scala"

name := "stealthnet-core"

version := "0.1.2-SNAPSHOT"

scalaVersion := "2.11.1"

scalacOptions ++= Seq("-deprecation", "-feature", "-optimize", "-unchecked", "-Yinline-warnings")

scalacOptions in (Compile, doc) ++= Seq("-diagrams", "-implicits")

val versions = Map[String, String](
  "java" -> "1.8",
  "akka" -> "2.3.3",
  "bouncycastle" -> "1.50",
  "config" -> "1.2.1",
  "junit" -> "4.11",
  "logback" -> "1.1.2",
  "netty" -> "4.0.20.Final",
  "scala-xml" -> "1.0.2",
  "scalatest" -> "2.2.0",
  "slf4j" -> "1.7.7",
  "maven-compiler-plugin" -> "3.1",
  "maven-dependency-plugin" -> "2.8",
  "maven-surefire-plugin" -> "2.17",
  "scala-maven-plugin" -> "3.1.6"
)

libraryDependencies ++= Seq(
  /* Log */
  "org.slf4j" % "slf4j-api" % versions("slf4j"),
  "ch.qos.logback" % "logback-classic" % versions("logback") % "runtime",
  /* Scala libraries */
  "org.scala-lang.modules" %% "scala-xml" % versions("scala-xml"),
  /* Akka */
  "com.typesafe.akka" %% "akka-actor" % versions("akka"),
  /* Configuration management */
  "com.typesafe" % "config" % versions("config"),
  /* Security (cryptography) */
  "org.bouncycastle" % "bcprov-jdk15on" % versions("bouncycastle"),
  /* Network */
  "io.netty" % "netty-all" % versions("netty"),
  /* Testing */
  "junit" % "junit" % versions("junit") % "test",
  "org.scalatest" %% "scalatest" % versions("scalatest") % "test",
  "com.typesafe.akka" %% "akka-testkit" % versions("akka") % "test"
)

org.scalastyle.sbt.ScalastylePlugin.Settings

org.scalastyle.sbt.PluginKeys.config := file("project/scalastyle-config.xml")


publishMavenStyle := true

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath + "/.m2/repository")))

pomExtra := (
  <properties>
    <encoding>UTF-8</encoding>
  </properties>
  <build>
    <sourceDirectory>src/main/scala</sourceDirectory>
    <testSourceDirectory>src/test/scala</testSourceDirectory>
    <plugins>
      <plugin>
        <groupId>net.alchim31.maven</groupId>
        <artifactId>scala-maven-plugin</artifactId>
        <version>{ versions("scala-maven-plugin") }</version>
        <configuration>
          <args>
            <arg>-deprecation</arg>
            <arg>-feature</arg>
            <arg>-Yinline-warnings</arg>
            <arg>-optimize</arg>
            <arg>-unchecked</arg>
          </args>
        </configuration>
        <executions>
          <execution>
            <goals>
              <goal>compile</goal>
              <goal>testCompile</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>{ versions("maven-compiler-plugin") }</version>
        <configuration>
          <source>{ versions("java") }</source>
          <target>{ versions("java") }</target>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>{ versions("maven-surefire-plugin") }</version>
        <configuration>
          <includes>
            <include>**/*Suite.class</include>
          </includes>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <version>{ versions("maven-dependency-plugin") }</version>
        <executions>
          <execution>
            <id>copy-dependencies</id>
            <phase>package</phase>
            <goals>
              <goal>copy-dependencies</goal>
            </goals>
            <configuration>
              <outputDirectory>${{project.build.directory}}/lib</outputDirectory>
              <overWriteReleases>false</overWriteReleases>
              <overWriteSnapshots>false</overWriteSnapshots>
              <overWriteIfNewer>true</overWriteIfNewer>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
    <!-- Ignore plugin goals not supported by m2e -->
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.eclipse.m2e</groupId>
          <artifactId>lifecycle-mapping</artifactId>
          <version>1.0.0</version>
          <configuration>
            <lifecycleMappingMetadata>
              <pluginExecutions>
                <pluginExecution>
                  <pluginExecutionFilter>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <versionRange>[1.0.0,)</versionRange>
                    <goals>
                      <goal>copy-dependencies</goal>
                    </goals>
                  </pluginExecutionFilter>
                  <action>
                    <ignore />
                  </action>
                </pluginExecution>
              </pluginExecutions>
            </lifecycleMappingMetadata>
          </configuration>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
)

