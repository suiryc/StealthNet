package perso.stealthnet.network

import org.jboss.netty.channel.{
  ChannelHandlerContext,
  ChannelStateEvent,
  SimpleChannelHandler
}
import perso.stealthnet.core.Core
import perso.stealthnet.network.protocol.commands.RSAParametersServerCommand
import perso.stealthnet.util.{EmptyLoggingContext, Logging}

class ConnectionLimitHandler
  extends SimpleChannelHandler
  with Logging
  with EmptyLoggingContext
{

  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    val cnx = StealthNetConnectionsManager.getConnection(e.getChannel)

    /* Note: for server-side, we would like to reject the connection as soon as
     * possible (e.g. upon channel opening), but this does not seem to work as
     * well as expected.
     * So just wait for the connection to be done to cleanly close it.
     */
    if (!StealthNetConnectionsManager.addConnection(cnx)) {
      e.getChannel.close
      return
    }

    /* server starts handshake */
    if (!cnx.isClient)
      e.getChannel.write(new RSAParametersServerCommand())

    super.channelConnected(ctx, e)
  }

  override def channelClosed(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    /* Cleanup the connection */
    StealthNetConnectionsManager !? StealthNetConnectionsManager.ClosedChannel(e.getChannel)
    super.channelClosed(ctx, e)
  }

  override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    val cnx = StealthNetConnectionsManager.getConnection(e.getChannel, false)

    if ((cnx != null) && !cnx.closing && !Core.stopping && (cnx.isClient || cnx.accepted))
      logger debug(cnx.loggerContext, "Remote host disconnected")

    super.channelDisconnected(ctx, e)
  }

}
