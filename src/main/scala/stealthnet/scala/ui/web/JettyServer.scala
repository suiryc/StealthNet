package stealthnet.scala.ui.web

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import stealthnet.scala.Config
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}

/**
 * @todo Documentation
 * @todo rename to WebServer ?
 */
object JettyServer extends Logging with EmptyLoggingContext {

  private var server: Server = null

  /** Starts server. */
  def start() {
    logger debug "Starting"

    server = new Server(Config.webServerPort)
 
    val context = new WebAppContext()
    /* XXX - decide path for final application */
    context.setDescriptor("src/main/webapp/WEB-INF/web.xml")
    context.setResourceBase("src/main/webapp")
    context.setContextPath("/")
    context.setParentLoaderPriority(true)
 
    server.setHandler(context)
 
    server.start()
  }

  /** Stops server. */
  def stop() {
    logger debug "Stopping"

    if (server != null) {
      server.stop()
      server.join()
    }

    logger debug "Stopped"
  }

}
