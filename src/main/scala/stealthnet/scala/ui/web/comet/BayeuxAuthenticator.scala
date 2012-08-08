package stealthnet.scala.ui.web.comet

import javax.faces.context.FacesContext
import org.cometd.server.DefaultSecurityPolicy
import org.cometd.bayeux.server.{BayeuxServer, ServerMessage, ServerSession}
import scala.collection.JavaConversions._
import com.primefaces.sample.beans.UserSession

class BayeuxAuthenticator
  extends DefaultSecurityPolicy
  with ServerSession.RemoveListener
{

  private def authenticate(server: BayeuxServer, session: ServerSession,
      message: ServerMessage): Boolean =
  {
    /* Allow local (i.e. server-side) sessions */
    if (session.isLocalSession)
      return true

    /* Get message 'ext.authentication' data */
    val ext = message.getExt
    if (ext == null)
      return false

    val authentication = ext.get("authentication").asInstanceOf[java.util.Map[String, Object]]
    if (authentication == null)
      return false

    /* Get session id from given authentication data */
    val sessionId = authentication.get("sessionId").asInstanceOf[String]
    if (sessionId == null)
      return false

    /* Check session id is known and user is logged */
    /* XXX - should we also check remote address ? */
    SessionManager.getSession(sessionId) match {
      case Some(session) =>
        val userSession = session.getAttribute("userSession").asInstanceOf[UserSession]
        if ((userSession == null) || !userSession.getLogged)
          return false

      case None =>
        return false
    }

    true
  }

  override def canHandshake(server: BayeuxServer, session: ServerSession,
      message: ServerMessage): Boolean =
  {
    val authenticated = authenticate(server, session, message)

    /* Be notified when the session disappears ('removed' callback) */
    if (authenticated)
      session.addListener(this)

    authenticated
  }

  override def removed(session: ServerSession, expired: Boolean) {
    ConnectionsUpdaterServer ! ConnectionsUpdaterServer.ActorLeft(session)
  }

}
