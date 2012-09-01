dirScript=$(dirname "$0")
cd "${dirScript}"
CLASSPATH_APP=${dirScript}/config:${dirScript}/lib/*:${dirScript}/webapp/WEB-INF/lib/stealthnet-ui-web-jsf-0.1.0.jar

# http://wiki.eclipse.org/Jetty/Howto/Configure_JSP
java -Dorg.apache.jasper.compiler.disablejsr199=true -cp "${CLASSPATH_APP}" stealthnet.scala.ui.web.sandbox.TestServer
