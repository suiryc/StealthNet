package stealthnet.scala.ui.web.comet

import org.cometd.bayeux.Message
import org.cometd.bayeux.server.{BayeuxServer, ServerSession}
import org.cometd.server.AbstractService
import scala.collection.JavaConversions._
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}

class NotificationsService(bayeux: BayeuxServer)
  extends AbstractService(bayeux, "notifications")
  with NotificationsManager
  with Logging
  with EmptyLoggingContext
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
        logger trace s"Connections notifications service: registering[$active] session[$remote]"
        if (active) ConnectionsNotificationsManager.register(remote)
        else ConnectionsNotificationsManager.unregister(remote)

      case _ =>
        val output = Map[String, Object](
          "level" -> "error",
          "message" -> s"Unknown channel[$channel]"
        )
        deliver(remote, output)
        return
    }
  }

}
