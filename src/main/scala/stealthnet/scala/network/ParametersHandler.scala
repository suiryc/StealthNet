package stealthnet.scala.network

import io.netty.channel.{ChannelDuplexHandler, ChannelHandlerContext}
import stealthnet.scala.network.connection.{
  StealthNetConnectionParameters,
  StealthNetConnectionsManager
}

/**
 * Inbound/outbound parameters handler.
 *
 * The sole purpose of this handler is to be used first in the handlers list so
 * that it can initialize a [[stealthnet.scala.network.connection.StealthNetConnection]]
 * for each new connection before other handlers access it.
 *
 * On server side, the channel is also added to a `ChannelGroup`.
 */
class ParametersHandler(val parameters: StealthNetConnectionParameters)
  extends ChannelDuplexHandler
{

  /**
   * Handles channel opening event.
   *
   * Creates and initializes a new
   * [[stealthnet.scala.network.connection.StealthNetConnection]] associated to
   * the connected channel.
   *
   * @see [[stealthnet.scala.network.connection.StealthNetConnectionsManager]].`getConnection`
   */
  override def channelActive(ctx: ChannelHandlerContext) {
    val cnx = StealthNetConnectionsManager.connection(ctx.channel)

    cnx.group = parameters.group
    cnx.isClient = parameters.isClient

    cnx.group foreach { _.add(cnx.channel) }

    super.channelActive(ctx)
  }

}
