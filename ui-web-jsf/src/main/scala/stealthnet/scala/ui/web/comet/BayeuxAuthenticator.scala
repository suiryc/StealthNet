package stealthnet.scala.ui.web.comet

import org.cometd.server.DefaultSecurityPolicy
import org.cometd.bayeux.server.{BayeuxServer, ServerMessage, ServerSession}
import stealthnet.scala.ui.web.beans.UserSession

class BayeuxAuthenticator
  extends DefaultSecurityPolicy
  with ServerSession.RemoveListener
{

  private def authenticate(server: BayeuxServer, session: ServerSession,
      message: ServerMessage): Boolean =
    /* Allow local (i.e. server-side) sessions */
    if (session.isLocalSession) true
    else Option(message.getExt).flatMap { ext =>
      /* Get message 'ext.authentication' data */
      Option(ext.get("authentication").asInstanceOf[java.util.Map[String, Object]])
    }.flatMap { authentication =>
      /* Get session id from given authentication data */
      Option(authentication.get("sessionId").asInstanceOf[String])
    }.flatMap { sessionId =>
      /* Get session from given id; and thus check this id is known */
      SessionManager.getSession(sessionId)
    }.flatMap { session =>
      /* Get user session from given session */
      Option(session.getAttribute("userSession").asInstanceOf[UserSession])
    }.exists { userSession =>
      /* Check user is logged */
      /* XXX - should we also check remote address ? */
      userSession.getLogged
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
    ConnectionsNotificationsManager.actor ! ConnectionsNotificationsManager.ActorLeft(session)
  }

}
