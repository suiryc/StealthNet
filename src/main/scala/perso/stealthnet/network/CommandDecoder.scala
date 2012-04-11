package perso.stealthnet.network

import javax.crypto.{Cipher, CipherInputStream}
import com.weiglewilczek.slf4s.Logging
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBufferInputStream}
import org.jboss.netty.channel.{Channel, ChannelHandlerContext}
import org.jboss.netty.handler.codec.replay.{ReplayingDecoder, VoidEnum}
import perso.stealthnet.core.cryptography.Hash
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
      val header = new String(buf.readBytes(Constants.protocol.length).array, "US-ASCII")
      /* XXX - cleanly handle issues */
      if (header != Constants.protocol) {
        logger debug("Bytes: " + (header.getBytes("US-ASCII").toList ::: buf.readBytes(buf.readableBytes).array.toList).toArray)
        throw new Exception("Invalid protocol header[" + header + "]")
      }
      checkpoint(CommandDecoderState.Encryption)
      null

    case CommandDecoderState.Encryption =>
      encryption = buf.readByte()
      /* XXX - check against known encryption methods */
      checkpoint(CommandDecoderState.Length)
      null

    case CommandDecoderState.Length =>
      length = ProtocolStream.convertShort(buf.readBytes(2).array)
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
