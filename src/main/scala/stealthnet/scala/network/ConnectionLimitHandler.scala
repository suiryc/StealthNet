package stealthnet.scala.network

import io.netty.channel.{ChannelInboundHandlerAdapter, ChannelHandlerContext}
import stealthnet.scala.core.Core
import stealthnet.scala.network.connection.{
  StealthNetConnection,
  StealthNetConnectionsManager
}
import stealthnet.scala.network.protocol.commands.RSAParametersServerCommand
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}

/**
 * Upstream connection limiter.
 */
class ConnectionLimitHandler
  extends ChannelInboundHandlerAdapter
  with Logging
  with EmptyLoggingContext
{

  /**
   * Handles channel connection event.
   *
   * Registers the [[stealthnet.scala.network.connection.StealthNetConnection]]
   * associated to the connected channel.
   * If connection limit is reached, the channel is closed.
   *
   * On server side, we start handshaking by sending our
   * [[stealthnet.scala.network.protocol.commands.RSAParametersServerCommand]].
   *
   * @note On server side, we would like to reject the connection as soon as
   *   possible (e.g. upon channel opening), but this does not seem to work as
   *   well as expected.
   *   So we just wait for the channel to be connected to cleanly close it.
   * @see [[stealthnet.scala.network.connection.StealthNetConnectionsManager]].`addConnection`
   */
  override def channelActive(ctx: ChannelHandlerContext) {
    val cnx = StealthNetConnectionsManager.connection(ctx.channel)

    if (!StealthNetConnectionsManager.addConnection(cnx)) {
      cnx.close()
      return
    }

    /* server starts handshake */
    if (!cnx.isClient)
      cnx.channel.write(new RSAParametersServerCommand())

    super.channelActive(ctx)
  }

  /**
   * Handles channel closing event.
   *
   * Unregisters the [[stealthnet.scala.network.connection.StealthNetConnection]]
   * associated to the channel.
   *
   * @see [[stealthnet.scala.network.connection.StealthNetConnectionsManager]].`closedConnection`
   */
  override def channelInactive(ctx: ChannelHandlerContext) {
    val cnx = StealthNetConnectionsManager.getConnection(ctx.channel)
    val loggerContext = StealthNetConnection.loggerContext(cnx, ctx.channel)

    if (cnx map(cnx => !cnx.closing && !Core.stopping && (cnx.isClient || cnx.accepted)) getOrElse(false))
      logger debug(loggerContext, "Remote host disconnected")
    else
      logger debug(loggerContext, "Disconnected")

    /* Cleanup the connection */
    StealthNetConnectionsManager.closedChannel(ctx.channel)
    super.channelInactive(ctx)
  }

}
