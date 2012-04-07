package perso.stealthnet.network

import com.weiglewilczek.slf4s.Logging
import org.jboss.netty.buffer.{
  ChannelBuffer,
  ChannelBuffers
}
import org.jboss.netty.channel.{
  Channels,
  ChannelHandlerContext,
  ExceptionEvent,
  MessageEvent,
  SimpleChannelHandler
}
import org.jboss.netty.channel.group.ChannelGroup

class CommandHandler(val group: ChannelGroup) extends SimpleChannelHandler with Logging {

  def channelOpen(ctx: ChannelHandlerContext, e: MessageEvent) = {
    if (group != null) {
    	group.add(e.getChannel)
    }
  }

  def channelConnected(ctx: ChannelHandlerContext, e: MessageEvent) = {
    /* XXX - handshake */
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) = {
    val m: ChannelBuffer = e.getMessage.asInstanceOf[ChannelBuffer]
    /* XXX - do something */
    val v = m.readInt
    logger debug "Received message: " + v
    e.getChannel().write(v + 1)
  }

  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) = {
    val v: Int = e.getMessage.asInstanceOf[Int]
    val buf: ChannelBuffer = ChannelBuffers.dynamicBuffer(4)

    buf.writeInt(v)

    Channels.write(ctx, e.getFuture, buf)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) = {
    logger warn ("Unexpected exception!",  e.getCause)
    e.getChannel.close
  }

}
