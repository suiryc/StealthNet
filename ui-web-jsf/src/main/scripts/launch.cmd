@echo off

SET DIR_SCRIPT=%~dp0
CD /D "%DIR_SCRIPT%"
SET CLASSPATH_APP=%DIR_SCRIPT%config;%DIR_SCRIPT%lib\*;%DIR_SCRIPT%webapp\WEB-INF\lib\stealthnet-ui-web-jsf_2.11-0.1.2-SNAPSHOT.jar

@REM http://www.eclipse.org/jetty/documentation/current/configuring-jsp.html
java -Dorg.apache.jasper.compiler.disablejsr199=true -cp "%CLASSPATH_APP%" stealthnet.scala.ui.web.sandbox.TestServer
