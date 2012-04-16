package perso.stealthnet.network

import java.net.InetSocketAddress
import org.jboss.netty.channel.{
  ChannelHandlerContext,
  ChannelStateEvent,
  SimpleChannelHandler
}
import org.jboss.netty.channel.ChannelEvent
import org.jboss.netty.channel.ChannelState

class ParametersHandler(val parameters: StealthNetConnectionParameters) extends SimpleChannelHandler {

  override def channelOpen(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    val cnx = StealthNetConnections.get(e.getChannel)

    cnx.group = parameters.group
    cnx.isClient = parameters.isClient

    if (cnx.group != null)
      cnx.group.add(e.getChannel)

    super.channelOpen(ctx, e)
  }

}
