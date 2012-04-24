package stealthnet.scala.network

import org.jboss.netty.channel.{
  ChannelHandlerContext,
  ChannelStateEvent,
  SimpleChannelHandler
}
import org.jboss.netty.channel.ChannelEvent
import org.jboss.netty.channel.ChannelState

/**
 * Upstream/downstream parameters handler.
 *
 * The sole purpose of this handler is to be used first in the handlers list so
 * that it can initialize a [[stealthnet.scala.network.StealthNetConnection]]
 * for each new connection before other handlers access it.
 *
 * On server side, the channel is also added to a `ChannelGroup`.
 */
class ParametersHandler(val parameters: StealthNetConnectionParameters)
  extends SimpleChannelHandler
{

  /**
   * Handles channel opening event.
   *
   * Creates and initializes a new
   * [[stealthnet.scala.network.StealthNetConnection]] associated to the
   * connected channel.
   *
   * @see [[stealthnet.scala.network.StealthNetConnectionsManager]].`getConnection`
   */
  override def channelOpen(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    val cnx = StealthNetConnectionsManager.connection(e.getChannel)

    cnx.group = parameters.group
    cnx.client = parameters.client
    cnx.peer = parameters.peer

    if (cnx.group != null)
      cnx.group.add(cnx.channel)

    super.channelOpen(ctx, e)
  }

}
