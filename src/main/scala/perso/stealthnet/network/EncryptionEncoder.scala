package perso.stealthnet.network

import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import org.jboss.netty.channel.{
  Channels,
  ChannelHandlerContext,
  MessageEvent,
  SimpleChannelDownstreamHandler
}

class EncryptionEncoder extends SimpleChannelDownstreamHandler {

  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) = {
    val buf: ChannelBuffer = e.getMessage.asInstanceOf[ChannelBuffer]

    /* XXX - really encode */
    val encoded: ChannelBuffer = ChannelBuffers.dynamicBuffer(buf.readableBytes)
    encoded.writeInt(buf.readableBytes)
    encoded.writeBytes(buf)

    Channels.write(ctx, e.getFuture, encoded)
  }

}
