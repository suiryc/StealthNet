package stealthnet.scala.network

import org.jboss.netty.channel.{
  ChannelHandlerContext,
  ChannelStateEvent,
  SimpleChannelUpstreamHandler
}
import stealthnet.scala.core.Core
import stealthnet.scala.network.protocol.commands.RSAParametersServerCommand
import stealthnet.scala.util.{EmptyLoggingContext, Logging}

/**
 * Upstream connection limiter.
 */
class ConnectionLimitHandler
  extends SimpleChannelUpstreamHandler
  with Logging
  with EmptyLoggingContext
{

  /**
   * Handles channel connection event.
   *
   * Registers the [[stealthnet.scala.network.StealthNetConnection]] associated
   * to the connected channel.
   * If connection limit is reached, the channel is closed.
   *
   * On server side, we start handshaking by sending our
   * [[stealthnet.scala.network.protocol.commands.RSAParametersServerCommand]].
   *
   * @note On server side, we would like to reject the connection as soon as
   *   possible (e.g. upon channel opening), but this does not seem to work as
   *   well as expected.
   *   So we just wait for the channel to be connected to cleanly close it.
   * @see [[stealthnet.scala.network.StealthNetConnectionsManager]].`addConnection`
   */
  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    val cnx = StealthNetConnectionsManager.connection(e.getChannel)

    if (!StealthNetConnectionsManager.addConnection(cnx)) {
      cnx.close()
      return
    }

    /* server starts handshake */
    if (!cnx.isClient)
      cnx.channel.write(new RSAParametersServerCommand())

    super.channelConnected(ctx, e)
  }

  /**
   * Handles channel closing event.
   *
   * Unregisters the [[stealthnet.scala.network.StealthNetConnection]]
   * associated to the channel.
   *
   * @see [[stealthnet.scala.network.StealthNetConnectionsManager]].`closedConnection`
   */
  override def channelClosed(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    /* Cleanup the connection */
    StealthNetConnectionsManager.closedChannel(e.getChannel)
    super.channelClosed(ctx, e)
  }

  /**
   * Handles channel disconnection event.
   *
   * Simply logs disconnection.
   */
  override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    val cnx = StealthNetConnectionsManager.getConnection(e.getChannel) match {
      case Some(cnx) => cnx
      case None => null
    }

    if ((cnx != null) && !cnx.closing && !Core.stopping && (cnx.isClient || cnx.accepted)) {
      logger debug(cnx.loggerContext, "Remote host disconnected")
      /* XXX - necessary, not done automatically ? */
      cnx.close()
    }
    else
      logger debug(if (cnx != null) cnx.loggerContext else List("remote" -> e.getChannel.getRemoteAddress), "Disconnected")

    super.channelDisconnected(ctx, e)
  }

}
