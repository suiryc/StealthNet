package perso.stealthnet.network

import com.weiglewilczek.slf4s.Logging
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.{
  Channel,
  ChannelHandlerContext
}
import org.jboss.netty.handler.codec.frame.FrameDecoder

class EncryptionDecoder extends FrameDecoder with Logging {

  protected def decode(ctx: ChannelHandlerContext, channel: Channel,
      buffer: ChannelBuffer): Object = {
    if (buffer.readableBytes < 4) {
      null
    }
    else {    
      buffer.readBytes(buffer.readableBytes)
    }
  }

}
