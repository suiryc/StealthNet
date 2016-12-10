package stealthnet.scala.ui.web.comet

import org.cometd.bayeux.server.ServerSession
import scala.collection.JavaConverters._

trait NotificationsManager {

  protected def deliver(session: ServerSession, channel: String, data: Map[String, Object]) {
    val output = Map[String, Object](
      "channel" -> channel,
      "data" -> (data.asJava:java.util.Map[String, Object])
    )

    // scalastyle:off null
    session.deliver(session, "/notifications", output.asJava:java.util.Map[String, Object])
    // scalastyle:on null
  }

}
