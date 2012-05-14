package com.primefaces.sample.cometd

import javax.servlet.{
  GenericServlet,
  ServletException,
  ServletRequest,
  ServletResponse
}
import org.cometd.bayeux.server.BayeuxServer

class BayeuxInitializer extends GenericServlet {

  override def init()  {
    val bayeux = getServletContext.getAttribute(BayeuxServer.ATTRIBUTE).asInstanceOf[BayeuxServer]
    new HelloService(bayeux)
  }

  def service(request: ServletRequest, response: ServletResponse) =
    throw new ServletException()

}