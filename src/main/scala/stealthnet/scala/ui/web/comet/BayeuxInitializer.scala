package stealthnet.scala.ui.web.comet

import javax.servlet.{
  GenericServlet,
  ServletException,
  ServletRequest,
  ServletResponse
}
import org.cometd.bayeux.server.BayeuxServer

class BayeuxInitializer extends GenericServlet {

  override def init() {
    val bayeux = getServletContext.getAttribute(BayeuxServer.ATTRIBUTE)
      .asInstanceOf[BayeuxServer]

    bayeux.setSecurityPolicy(new BayeuxAuthenticator())

    new ConnectionsUpdaterService(bayeux)
  }

  def service(request: ServletRequest, response: ServletResponse) =
    throw new ServletException()

}
