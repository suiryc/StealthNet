package perso.stealthnet.network

import java.net.InetSocketAddress
import com.weiglewilczek.slf4s.Logging
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import org.jboss.netty.channel.{
  ChannelEvent,
  ChannelHandlerContext,
  Channels,
  ChannelState,
  ChannelStateEvent,
  SimpleChannelHandler
}
import perso.stealthnet.network.protocol.RSAParametersCommand

class ConnectionLimitHandler extends SimpleChannelHandler with Logging {

  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    val cnx = StealthNetConnections.get(e.getChannel)

    /* Note: for server-side, we would like to reject the connection as soon as
     * possible (e.g. upon channel opening), but this does not seem to work as
     * well as expected.
     * So just wait for the connection to be done to cleanly close it.
     */
    if (!StealthNetConnections.accept(cnx)) {
      e.getChannel.close
      return
    }

    /* 'handshake' */
    e.getChannel.write(new RSAParametersCommand())

    super.channelConnected(ctx, e)
  }

  override def channelClosed(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    /* Cleanup the connection */
    StealthNetConnections.closed(e.getChannel)
    super.channelClosed(ctx, e)
  }

  override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    val cnx = StealthNetConnections.get(e.getChannel, create = false)

    if ((cnx != null) && !cnx.closing && (cnx.isClient || cnx.accepted))
      logger debug("Host[" + cnx.host + "] disconnected")

    super.channelDisconnected(ctx, e)
  }

}
