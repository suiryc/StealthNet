package stealthnet.scala.ui.web.comet

import org.cometd.bayeux.Message
import org.cometd.bayeux.server.{BayeuxServer, ServerSession}
import org.cometd.server.AbstractService
import scala.collection.JavaConversions._

class NotificationsService(bayeux: BayeuxServer)
  extends AbstractService(bayeux, "notifications")
  with NotificationsManager
{

  addService("/service/notifications", "processClient")

  private def deliver(session: ServerSession, data: Map[String, Object]): Unit =
    deliver(session, "global", data)

  def processClient(remote: ServerSession, message: Message) {
    val data = message.getDataAsMap().asInstanceOf[java.util.Map[java.lang.String, java.lang.String]]

    val channel = Option(data.get("channel")) match {
      case None =>
        val output = Map[String, Object](
          "level" -> "error",
          "message" -> "Missing mandatory paramater: channel"
        )
        deliver(remote, output)
        return

      case Some(channel) =>
        channel
    }

    val active = Option(data.get("active")) map {
      _.toBoolean
    } getOrElse {
      false
    }

    channel match {
      case "connections" =>
        if (active)
          new ConnectionsNotificationsManager(remote)
        else
          ConnectionsNotificationsManager ! ConnectionsNotificationsManager.ActorLeft(remote)

      case _ =>
        val output = Map[String, Object](
          "level" -> "error",
          "message" -> ("Unknown channel[" + channel + "]")
        )
        deliver(remote, output)
        return
    }
  }

}
