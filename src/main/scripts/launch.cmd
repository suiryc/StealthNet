@echo off

SET DIR_SCRIPT=%~dp0
CD /D "%DIR_SCRIPT%"
SET CLASSPATH_APP=%DIR_SCRIPT%lib\*;%DIR_SCRIPT%webapp\WEB-INF\lib\test-jsf-primefaces-0.1.0.jar

@REM http://wiki.eclipse.org/Jetty/Howto/Configure_JSP
java -Dorg.apache.jasper.compiler.disablejsr199=true -cp "%CLASSPATH_APP%" com.primefaces.sample.TestServer
@REM java -cp "%CLASSPATH_APP%" com.primefaces.sample.TestServer
