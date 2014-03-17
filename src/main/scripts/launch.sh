dirScript=$(dirname "$0")
cd "${dirScript}"
dirScript=$(pwd)
CLASSPATH_APP=${dirScript}/config:${dirScript}/lib/*:${dirScript}/webapp/WEB-INF/lib/stealthnet-ui-web-jsf-0.1.0.jar

# http://www.eclipse.org/jetty/documentation/current/configuring-jsp.html
java -Dorg.apache.jasper.compiler.disablejsr199=true -cp "${CLASSPATH_APP}" stealthnet.scala.ui.web.sandbox.TestServer
