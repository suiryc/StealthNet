package perso.stealthnet.network

import javax.crypto.{Cipher, CipherInputStream}
import com.weiglewilczek.slf4s.Logging
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBufferInputStream}
import org.jboss.netty.channel.{Channel, ChannelHandlerContext}
import org.jboss.netty.handler.codec.replay.{ReplayingDecoder, VoidEnum}
import perso.stealthnet.network.protocol.{Constants, ProtocolStream}

object CommandDecoderState extends Enumeration {
  val Header, Encryption, Length, Content = Value
}

class CommandDecoder extends ReplayingDecoder[VoidEnum] with Logging {

  private var state = CommandDecoderState.Header
  private var encryption: Byte = _
  private var length: Int = _

  /**
   * Checkpoints.
   */
  protected def checkpoint(newState: CommandDecoderState.Value) {
    checkpoint()
    state = newState
  }

  override protected def decode(ctx: ChannelHandlerContext, channel: Channel,
      buf: ChannelBuffer, state_unused: VoidEnum): Object = {
    state match {
    case CommandDecoderState.Header =>
      var headerLength = ProtocolStream.convertShort(buf.readBytes(2).array)
      /* XXX - cleanly handle issues */
      if (headerLength != Constants.protocol.length)
        throw new Exception("Invalid protocol header length[" + headerLength + "]")
      var header = new String(buf.readBytes(headerLength).array, "US-ASCII")
      if (header != Constants.protocol)
        throw new Exception("Invalid protocol header[" + header + "]")
      logger debug ("Got protocol header")
      checkpoint(CommandDecoderState.Encryption)
      null

    case CommandDecoderState.Encryption =>
      encryption = buf.readByte()
      /* XXX - check against known encryption methods */
      logger debug ("Got encryption[0x%02X]".format(encryption))
      checkpoint(CommandDecoderState.Length)
      null

    case CommandDecoderState.Length =>
      length = ProtocolStream.convertShort(buf.readBytes(2).array)
      logger debug ("Got command length[" + length + "]")
      checkpoint(CommandDecoderState.Content)
      null

    case CommandDecoderState.Content =>
      val content: ChannelBuffer = buf.readBytes(length)
      checkpoint(CommandDecoderState.Header)
      /* cipher-text section */
      /* XXX - get reading cipher of connection */
      val input = new ChannelBufferInputStream(content)
      val cipher: Cipher = null
      if (cipher != null)
        new CipherInputStream(input, cipher)
      else
        input
    }
  }

}
