package stealthnet.scala.network.connection

import io.netty.channel.Channel
import java.util.Date

/**  ''StealthNet'' connection companion object. */
object StealthNetConnection {

  def loggerContext(cnx: Option[StealthNetConnection], channel: Channel) = {
    val ctx = cnx map(_.loggerContext) getOrElse(Nil)

    if (ctx.find(_._1 == "peer").isDefined)
      ctx
    else
      ctx ::: List("remote" -> channel.remoteAddress)
  }

}

/**
 * ''StealthNet'' connection associated to a channel.
 */
class StealthNetConnection protected[connection] (val channel: Channel)
  extends StealthNetConnectionParameters()
{

  // scalastyle:off null
  assert(channel != null)
  // scalastyle:on null

  val createDate = new Date()

  /** Closes the connection channel. */
  def close() {
    closing = true
    channel.close()
  }

}
