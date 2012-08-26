package com.primefaces.sample

import java.io.File
import org.eclipse.jetty.server.{Server => jettyServer }
import org.eclipse.jetty.webapp.WebAppContext
import stealthnet.scala.core.Core
import stealthnet.scala.network.StealthNetConnectionsManager
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}
import stealthnet.scala.ui.web.Settings
import stealthnet.scala.ui.web.comet.{
  ConnectionsUpdaterServer,
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

    val context = new WebAppContext()
    context.setDescriptor("webapp/WEB-INF/web.xml")
    context.setResourceBase("webapp")
    context.setContextPath("/")
    context.setParentLoaderPriority(true)

    server.setHandler(context)

    server.start()

    SessionManager.start()
    ConnectionsUpdaterServer.start()

    StealthNetConnectionsManager !
      StealthNetConnectionsManager.AddConnectionsListener(ConnectionsUpdaterServer)

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
    ConnectionsUpdaterServer.stop()

    Core.stop()

    logger debug "Stopped"
  }

}
