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
    var encryptedDuplicate: ChannelBuffer = null
    try {
      val length = if (debug)
          builder.readHeader(cnx, new perso.stealthnet.util.DebugInputStream(new ChannelBufferInputStream(buf), cnx.loggerContext))
        else
          builder.readHeader(cnx, new ChannelBufferInputStream(buf))

      /* Note: checkpoint since we could read data and advance one step */
      checkpoint()
      if (length < 0)
        return null

      val encrypted = buf.readBytes(length)
      encryptedDuplicate = encrypted.duplicate()
      val command = if (debug)
          builder.readCommand(cnx, new perso.stealthnet.util.DebugInputStream(new ChannelBufferInputStream(encrypted), cnx.loggerContext ++ List("step" -> "encrypted")))
        else
          builder.readCommand(cnx, new ChannelBufferInputStream(encrypted))
      checkpoint()

      if (command == null)
        logData(cnx, encryptedDuplicate)

      command
    }
    catch {
      case _ if (cnx.closing || Core.stopping) =>
        /* nothing to say here */
        checkpoint()
        null

      /* XXX - closing channel is not done right away ... */
      case e: ProtocolException =>
        logger error(e.loggerContext, "Protocol exception", e)
        checkpoint()
        cnx.closing = true
        channel.close
        logData(cnx, encryptedDuplicate)
        null

      case e: EOFException =>
        logger error(cnx.loggerContext, "Reached End-Of-Stream", e)
        checkpoint()
        cnx.closing = true
        channel.close
        logData(cnx, encryptedDuplicate)
        null
    }
  }

  private def logData(cnx: StealthNetConnection, buf: ChannelBuffer) {
    if (buf == null)
      return

    try {
      val decrypted = builder.decryptCommand(cnx, buf.array, buf.readerIndex, buf.readableBytes)
      logger debug(cnx.loggerContext, "Command was:\n" + HexDumper.dump(decrypted))
    }
    catch {
      case e =>
        logger debug(cnx.loggerContext, "Could not decrypt command, encrypted data was:\n" + HexDumper.dump(buf.array, buf.readerIndex, buf.readableBytes), e)
    }
  }

}
