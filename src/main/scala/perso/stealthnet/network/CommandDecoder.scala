package perso.stealthnet.network

import java.io.EOFException
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBufferInputStream}
import org.jboss.netty.channel.{Channel, ChannelHandlerContext}
import org.jboss.netty.handler.codec.replay.{ReplayingDecoder, VoidEnum}
import perso.stealthnet.core.Core
import perso.stealthnet.network.protocol.commands.Command
import perso.stealthnet.network.protocol.exceptions.ProtocolException
import perso.stealthnet.util.{EmptyLoggingContext, HexDumper, Logging}

class CommandDecoder
  extends ReplayingDecoder[VoidEnum]
  with Logging
  with EmptyLoggingContext
{

  /* XXX */
  private val debug = false

  private var builder: Command.Builder = new Command.Builder()

  override protected def decode(ctx: ChannelHandlerContext, channel: Channel,
      buf: ChannelBuffer, state_unused: VoidEnum): Object = {
    val cnx = StealthNetConnectionsManager.getConnection(channel)
    try {
      val length = if (debug)
          builder.readHeader(cnx, new perso.stealthnet.util.DebugInputStream(new ChannelBufferInputStream(buf), cnx.loggerContext))
        else
          builder.readHeader(cnx, new ChannelBufferInputStream(buf))
      if (length < 0) {
        checkpoint()
        return null
      }

      val encrypted = buf.readBytes(length)
      val encrypted2 = encrypted.duplicate()
      val command = if (debug)
          builder.readCommand(cnx, new perso.stealthnet.util.DebugInputStream(new ChannelBufferInputStream(encrypted), cnx.loggerContext))
        else
          builder.readCommand(cnx, new ChannelBufferInputStream(encrypted))
      checkpoint()

      if (command == null) {
        try {
          val decrypted = builder.decryptCommand(cnx, encrypted2.array, encrypted2.readerIndex, encrypted2.readableBytes)
          logger debug(cnx.loggerContext, "Command was:\n" + HexDumper.dump(decrypted))
        }
        catch {
          case e =>
            logger debug(cnx.loggerContext, "Could not decrypt command, encrypted data was:\n" + HexDumper.dump(encrypted2.array, encrypted2.readerIndex, encrypted2.readableBytes), e)
        }
      }

      command
    }
    catch {
      /* XXX - closing channel is not done right away ... */
      case e: ProtocolException =>
        logger error(e.loggerContext, "Protocol exception", e)
        checkpoint()
        channel.close
        null

      case _ if (cnx.closing || Core.stopping) =>
        /* nothing to say here */
        checkpoint()
        null

      case e: EOFException =>
        logger error(cnx.loggerContext, "Reached End-Of-Stream", e)
        checkpoint()
        channel.close
        null
    }
  }

}
