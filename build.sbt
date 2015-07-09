organization := "stealthnet.scala"

name := "stealthnet-ui-web-jsf"

version := "0.1.2-SNAPSHOT"

scalaVersion := versions("scala")

scalacOptions ++= Seq("-deprecation", "-feature", "-optimize", "-unchecked", "-Yinline-warnings")

scalacOptions in (Compile, doc) ++= Seq("-diagrams", "-implicits")

val versions = Map[String, String](
  "java" -> "1.8",
  "scala" -> "2.11.1",
  "akka" -> "2.3.3",
  "cometd" -> "3.0.0",
  "config" -> "1.2.1",
  "jetty" -> "9.2.1.v20140609",
  "jetty-jsp-jdt" -> "2.3.3",
  "logback" -> "1.1.2",
  "mojara" -> "2.2.7",
  "primefaces" -> "5.0",
  "slf4j" -> "1.7.7",
  "stealthnet-core" -> "0.1.2-SNAPSHOT",
  "maven-antrun-plugin" -> "1.7",
  "maven-compiler-plugin" -> "3.1",
  "maven-dependency-plugin" -> "2.8",
  "maven-jar-plugin" -> "2.5",
  "maven-resources-plugin" -> "2.6",
  "maven-surefire-plugin" -> "2.17",
  "scala-maven-plugin" -> "3.1.6"
)

libraryDependencies ++= Seq(
  /** Scala libraries */
  "org.scala-lang" % "scala-reflect" % versions("scala"),
  /** Log **/
  "org.slf4j" % "slf4j-api" % versions("slf4j"),
  "org.slf4j" % "jul-to-slf4j" % versions("slf4j"),
  "ch.qos.logback" % "logback-classic" % versions("logback") % "runtime",
  /** Akka **/
  "com.typesafe.akka" %% "akka-actor" % versions("akka"),
  /** Configuration management **/
  "com.typesafe" % "config" % versions("config"),
  /** Network **/
  "org.eclipse.jetty" % "jetty-server" % versions("jetty"),
  "org.eclipse.jetty" % "jetty-webapp" % versions("jetty"),
  "org.eclipse.jetty" % "jetty-jsp" % versions("jetty"),
  /* JSP compiler necessary with Jetty 9; previously provided by org.apache.jasper.glassfish through jetty-jsp */
  "org.eclipse.jetty.toolchain" % "jetty-jsp-jdt" % versions("jetty-jsp-jdt"),
  /* Useful for CometD support (which otherwise relies on earlier versions).
   * org.eclipse.jetty/jetty-websocket in Jetty 8 moved to org.eclipse.jetty.websocket/websocket-server in Jetty 9 */
  /*"org.eclipse.jetty" % "jetty-websocket" % versions("jetty"),*/
  /* Jetty 9 supports JSR 356 */
  "org.eclipse.jetty.websocket" % "javax-websocket-server-impl" % versions("jetty"),
  /*"org.eclipse.jetty", "jetty-client" % versions("jetty"),*/
  "org.eclipse.jetty" % "jetty-jmx" % versions("jetty"),
  /* jetty-util-ajax is separated from jetty-util in Jetty 9 */
  "org.eclipse.jetty" % "jetty-util-ajax" % versions("jetty"),
  /** JSF **/
  /* Mojara implementation (https://javaserverfaces.java.net/download.html)
   * Note: could be replaced by MyFaces implementation */
  "com.sun.faces" % "jsf-api" % versions("mojara"),
  "com.sun.faces" % "jsf-impl" % versions("mojara"),
  /* Alternative way ? */
  /*"javax.faces", "javax.faces-api" % versions("mojara") % "provided",
  "org.glassfish", "javax.faces" % versions("mojara") % "provided",*/
  /* PrimeFaces */
  "org.primefaces" % "primefaces" % versions("primefaces"),
  /** CometD **/
  "org.cometd.java" % "bayeux-api" % versions("cometd"),
  "org.cometd.java" % "cometd-java-server" % versions("cometd"),
  /* cometd-websocket-jetty in CometD 2.x moved to cometd-java-websocket-jetty-server in CometD 3.x
   * cometd-java-websocket-javax-server supports JSR 356 in CometD 3.x */
  "org.cometd.java" % "cometd-java-websocket-javax-server" % versions("cometd"),
  /** StealthNet **/
  "stealthnet.scala" %% "stealthnet-core" % versions("stealthnet-core")
)

resolvers += Resolver.mavenLocal

org.scalastyle.sbt.ScalastylePlugin.Settings

org.scalastyle.sbt.PluginKeys.config := file("project/scalastyle-config.xml")


publishMavenStyle := true

publishTo := Some(Resolver.mavenLocal)

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
          <execution>
            <id>integration-test:copy-dependencies</id>
            <phase>integration-test</phase>
            <goals>
              <goal>copy-dependencies</goal>
            </goals>
            <configuration>
              <outputDirectory>${{project.build.directory}}/dist/lib</outputDirectory>
              <overWriteReleases>false</overWriteReleases>
              <overWriteSnapshots>false</overWriteSnapshots>
              <overWriteIfNewer>true</overWriteIfNewer>
            </configuration>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-resources-plugin</artifactId>
        <version>{ versions("maven-resources-plugin") }</version>
        <executions>
          <execution>
            <id>integration-test:copy-resources</id>
            <phase>integration-test</phase>
            <goals>
              <goal>copy-resources</goal>
            </goals>
            <configuration>
              <outputDirectory>${{project.build.directory}}/dist</outputDirectory>
              <resources>
                <resource>
                  <directory>${{basedir}}/src/main</directory>
                  <includes>
                    <include>webapp/**</include>
                    <include>config/**</include>
                  </includes>
                </resource>
                <resource>
                  <directory>${{basedir}}/src/main/scripts</directory>
                </resource>
              </resources>
            </configuration>
          </execution>
        </executions>
      </plugin>
      <!-- maven-resources-plugin:copy-resources does not keep file permissions -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-antrun-plugin</artifactId>
        <version>{ versions("maven-antrun-plugin") }</version>
        <executions>
          <execution>
            <id>integration-test:run</id>
            <phase>integration-test</phase>
            <configuration>
              <target>
                <chmod file="${project.build.directory}/dist/launch.sh" perm="775"/>
              </target>
            </configuration>
            <goals>
              <goal>run</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <version>{ versions("maven-jar-plugin") }</version>
        <executions>
          <execution>
            <id>integration-test:jar</id>
            <phase>integration-test</phase>
            <goals>
              <goal>jar</goal>
            </goals>
            <configuration>
              <outputDirectory>${{project.build.directory}}/dist/webapp/WEB-INF/lib</outputDirectory>
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

