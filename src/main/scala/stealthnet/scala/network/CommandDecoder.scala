package stealthnet.scala.network

import java.io.EOFException
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBufferInputStream}
import org.jboss.netty.channel.{Channel, ChannelHandlerContext}
import org.jboss.netty.handler.codec.replay.{ReplayingDecoder, VoidEnum}
import stealthnet.scala.Config
import stealthnet.scala.core.Core
import stealthnet.scala.network.protocol.commands.Command
import stealthnet.scala.network.protocol.exceptions.ProtocolException
import stealthnet.scala.util.{
  DebugInputStream,
  EmptyLoggingContext,
  HexDumper,
  Logging
}

/**
 * Upstream command decoder.
 */
class CommandDecoder
  extends ReplayingDecoder[VoidEnum]
  with Logging
  with EmptyLoggingContext
{

  /** Command builder. */
  private var builder: Command.Builder = new Command.Builder()

  /**
   * Reads incoming data and rebuilds a
   * [[stealthnet.scala.network.protocol.commands.Command]].
   */
  override protected def decode(ctx: ChannelHandlerContext, channel: Channel,
    buf: ChannelBuffer, state_unused: VoidEnum): Object =
  {
    val cnx = StealthNetConnectionsManager.connection(channel)
    if (cnx.closing || Core.stopping) {
      /* drop data */
      buf.skipBytes(buf.readableBytes)
      checkpoint()
      return null
    }

    var encryptedDuplicate: ChannelBuffer = null
    try {
      val length = if (Config.debugIO)
          builder.readHeader(cnx, new DebugInputStream(new ChannelBufferInputStream(buf), cnx.loggerContext ++ List("step" -> "header")))
        else
          builder.readHeader(cnx, new ChannelBufferInputStream(buf))

      /* Note: checkpoint since we could read data and advance one step */
      checkpoint()
      if (length < 0)
        return null

      val encrypted = buf.readBytes(length)
      encryptedDuplicate = encrypted.duplicate()
      val command = if (Config.debugIO)
          builder.readCommand(cnx, new DebugInputStream(new ChannelBufferInputStream(encrypted), cnx.loggerContext ++ List("step" -> "encrypted")))
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

      case e =>
        logger error(cnx.loggerContext, "Protocol issue", e)
        checkpoint()
        /* Note: closing channel is not done right away ... */
        cnx.close()
        logData(cnx, encryptedDuplicate)
        null
    }
  }

  /**
   * Logs command data.
   *
   * Used upon issue to help investigate cause. Tries to decrypt data, otherwise
   * just logs encrypted data.
   *
   * @param cnx connection
   * @param buf channel buffer
   */
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
        logger debug(cnx.loggerContext, "Decrypting data are: " + builder.decryptingData(cnx))
    }
  }

}
