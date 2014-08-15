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
  {
    /* Allow local (i.e. server-side) sessions */
    if (session.isLocalSession)
      return true

    Option(message.getExt) flatMap { ext =>
      /* Get message 'ext.authentication' data */
      Option(ext.get("authentication").asInstanceOf[java.util.Map[String, Object]])
    } flatMap { authentication =>
      /* Get session id from given authentication data */
      Option(authentication.get("sessionId").asInstanceOf[String])
    } map { sessionId =>
      /* Check session id is known and user is logged */
      /* XXX - should we also check remote address ? */
      SessionManager.getSession(sessionId) map { session =>
        Option(session.getAttribute("userSession").asInstanceOf[UserSession]) map { userSession =>
          userSession.getLogged
        } getOrElse {
          false
        }
      } getOrElse {
        false
      }
    } getOrElse {
      false
    }
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
