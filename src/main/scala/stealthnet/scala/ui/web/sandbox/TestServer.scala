package stealthnet.scala.ui.web.sandbox

import org.slf4j.bridge.SLF4JBridgeHandler
import stealthnet.scala.ui.web.Server

// scalastyle:off regex
object TestServer {

  /* XXX - http://wiki.eclipse.org/Jetty/Howto/Configure_JSP
   */
  def main(args: Array[String]): Unit = {
    println("Started")

    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()

    Server.start()
  }

}
// scalastyle:on regex
