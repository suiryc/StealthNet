package perso.stealthnet.network

import java.io.EOFException
import javax.crypto.{Cipher, CipherInputStream}
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBufferInputStream}
import org.jboss.netty.channel.{Channel, ChannelHandlerContext}
import org.jboss.netty.handler.codec.replay.{ReplayingDecoder, VoidEnum}
import perso.stealthnet.core.cryptography.Hash
import perso.stealthnet.network.protocol.{Command, Constants, ProtocolStream}

class CommandDecoder extends ReplayingDecoder[VoidEnum] {

  private var builder: Command.Builder = new Command.Builder()

  override protected def decode(ctx: ChannelHandlerContext, channel: Channel,
      buf: ChannelBuffer, state_unused: VoidEnum): Object = {
    val cnx = StealthNetConnections.get(channel)
    try {
      val length = builder.readHeader(cnx, new ChannelBufferInputStream(buf))
      if (length < 0) {
        checkpoint()
        return null
      }

      builder.readCommand(cnx, new ChannelBufferInputStream(buf.readBytes(length)))
    }
    catch {
      case e: EOFException =>
        channel.close
        null
    }
  }

}
