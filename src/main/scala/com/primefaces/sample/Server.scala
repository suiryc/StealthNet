package com.primefaces.sample

import java.io.File
import org.eclipse.jetty.server.{Server => jettyServer }
import org.eclipse.jetty.webapp.WebAppContext
import stealthnet.scala.Config
import stealthnet.scala.core.Core
import stealthnet.scala.network.StealthNetConnectionsManager
import stealthnet.scala.ui.web.comet.ConnectionsUpdaterServer
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}
import stealthnet.scala.ui.web.comet.SessionManager

/**
 * @todo Documentation
 */
object Server extends Logging with EmptyLoggingContext {

  private var server: jettyServer = null

  /**
   * Starts server.
   */
  def start() {
    logger debug "Starting"

    server = new jettyServer(Config.webServerPort)
 
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

    if (server != null) {
      server.stop()
      server.join()
    }
    SessionManager.stop()
    ConnectionsUpdaterServer.stop()

    Core.stop()

    logger debug "Stopped"
  }

}
