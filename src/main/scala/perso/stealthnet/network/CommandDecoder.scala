package perso.stealthnet.network

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.{Channel, ChannelHandlerContext}
import org.jboss.netty.handler.codec.replay.{ReplayingDecoder, VoidEnum}

object CommandDecoderState extends Enumeration {
  val READ_LENGTH, READ_CONTENT = Value
}

class CommandDecoder extends ReplayingDecoder[VoidEnum] {

  private var state = CommandDecoderState.READ_LENGTH
  private var length: Int = _

  override protected def decode(ctx: ChannelHandlerContext, channel: Channel,
      buf: ChannelBuffer, state_unused: VoidEnum): Object = {

    /* XXX - shall return a Command object */
    state match {
    case CommandDecoderState.READ_LENGTH =>
      length = buf.readInt()
      checkpoint()
      state = CommandDecoderState.READ_CONTENT
      null

    case CommandDecoderState.READ_CONTENT =>
      val frame: ChannelBuffer = buf.readBytes(length)
      checkpoint()
      state = CommandDecoderState.READ_LENGTH
      frame
    }
  }

}
