package stealthnet.scala.ui.web

import org.eclipse.jetty.server.{Server => jettyServer}
import org.eclipse.jetty.webapp.WebAppContext
import stealthnet.scala.core.Core
import stealthnet.scala.network.connection.StealthNetConnectionsManager
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}
import stealthnet.scala.ui.web.comet.{
  ConnectionsNotificationsManager,
  SessionManager
}

/**
 * @todo Documentation
 */
object Server extends Logging with EmptyLoggingContext {

  private var server: Option[jettyServer] = None

  /**
   * Starts server.
   */
  def start() {
    logger debug "Starting"

    val server = new jettyServer(Settings.ui.webServerPort)
    this.server = Some(server)

    /* XXX - replaced by setStopTimeout(long) in Jetty 9 ? */
    server.setGracefulShutdown(Settings.ui.shutdownGracePeriod.intValue)
    server.setStopAtShutdown(true)

    val context = new WebAppContext()
    context.setDescriptor("webapp/WEB-INF/web.xml")
    context.setResourceBase("webapp")
    context.setContextPath("/")
    context.setParentLoaderPriority(true)

    server.setHandler(context)

    server.start()

    SessionManager.start()
    ConnectionsNotificationsManager.start()

    StealthNetConnectionsManager.addConnectionsListener(ConnectionsNotificationsManager)

    Core.start()
  }

  /**
   * Stops server.
   */
  def stop() {
    logger debug "Stopping"

    server foreach { server =>
      server.stop()
      server.join()
    }
    SessionManager.stop()
    ConnectionsNotificationsManager.stop()

    Core.stop()

    logger debug "Stopped"
  }

}
