package com.primefaces.sample

import java.io.File
import org.eclipse.jetty.server.{Server => jettyServer }
import org.eclipse.jetty.webapp.WebAppContext
import com.primefaces.sample.cometd.SessionManager

/**
 * @todo Documentation
 */
object Server {

  private var server: jettyServer = null

  /**
   * Starts server.
   */
  def start() {
    server = new jettyServer(8080)
 
    val context = new WebAppContext()
    context.setDescriptor("webapp/WEB-INF/web.xml")
    context.setResourceBase("webapp")
    context.setContextPath("/")
    context.setParentLoaderPriority(true)

    server.setHandler(context)
 
    server.start()

    SessionManager.start()
  }

  /**
   * Stops server.
   */
  def stop() {
    if (server != null) {
      server.stop()
      server.join()
    }
    SessionManager.stop()
  }

}
