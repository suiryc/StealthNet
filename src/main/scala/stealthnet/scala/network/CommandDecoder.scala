package stealthnet.scala.network

import java.io.EOFException
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBufferInputStream}
import org.jboss.netty.channel.{Channel, ChannelHandlerContext}
import org.jboss.netty.handler.codec.replay.{ReplayingDecoder, VoidEnum}
import stealthnet.scala.Settings
import stealthnet.scala.core.Core
import stealthnet.scala.network.connection.{
  StealthNetConnection,
  StealthNetConnectionsManager
}
import stealthnet.scala.network.protocol.commands.Command
import stealthnet.scala.network.protocol.exceptions.ProtocolException
import stealthnet.scala.util.HexDumper
import stealthnet.scala.util.io.DebugInputStream
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}

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
  // scalastyle:off method.length null
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

    var encryptedDuplicate: Option[ChannelBuffer] = None
    try {
      val length = if (Settings.core.debugIOData)
          builder.readHeader(cnx, new DebugInputStream(new ChannelBufferInputStream(buf), cnx.loggerContext ++ List("step" -> "header")))
        else
          builder.readHeader(cnx, new ChannelBufferInputStream(buf))

      /* Note: checkpoint since we could read data and advance one step */
      checkpoint()
      if (length < 0)
        return null

      val encrypted = buf.readBytes(length)
      encryptedDuplicate = Some(encrypted.duplicate())
      val command = if (Settings.core.debugIOData)
          builder.readCommand(cnx, new DebugInputStream(new ChannelBufferInputStream(encrypted), cnx.loggerContext ++ List("step" -> "encrypted")))
        else
          builder.readCommand(cnx, new ChannelBufferInputStream(encrypted))
      checkpoint()

      if (Settings.core.debugIOCommands && command.isDefined && logger.isTraceEnabled)
        encryptedDuplicate foreach { logData(cnx, false, _) }

      command getOrElse {
        encryptedDuplicate foreach { logData(cnx, true, _) }
        null
      }
    }
    catch {
      /* Note: do not catch Errors because ReplayError (which is protected) is
       * what is thrown by ReplayingDecoder buffer. */
      case e: Exception =>
        checkpoint()
        /* XXX - special case for EOFException ? */
        if (!cnx.closing && !Core.stopping) {
          logger error(cnx.loggerContext, "Protocol issue", e)
          /* Note: closing channel is not done right away ... */
          cnx.close()
          encryptedDuplicate foreach { logData(cnx, true, _) }
        }
        /* else: nothing to say here */
        null
    }
  }
  // scalastyle:on method.length null

  /**
   * Logs command data.
   *
   * Used upon debugging or to help investigate issues' cause. Tries to decrypt
   * data, otherwise just logs encrypted data.
   *
   * @param cnx   connection
   * @param issue whether we faced an issue
   * @param buf   channel buffer
   * @todo Find way to manage both TRACE and ERROR log level ?
   */
  private def logData(cnx: StealthNetConnection, issue: Boolean,
    buf: ChannelBuffer)
  {
    try {
      val decrypted = builder.decryptData(cnx, buf.array, buf.readerIndex, buf.readableBytes)
      if (issue)
        logger error(cnx.loggerContext, "Decrypted data:\n" + HexDumper.dump(decrypted))
      else
        logger trace(cnx.loggerContext, "Decrypted data:\n" + HexDumper.dump(decrypted))
    }
    catch {
      case e =>
        if (issue) {
          logger error(cnx.loggerContext, "Could not decrypt data:\n" + HexDumper.dump(buf.array, buf.readerIndex, buf.readableBytes), e)
          logger error(cnx.loggerContext, "Decrypting data are: " + builder.decryptingData(cnx))
        }
        else {
          logger trace(cnx.loggerContext, "Could not decrypt data:\n" + HexDumper.dump(buf.array, buf.readerIndex, buf.readableBytes), e)
          logger trace(cnx.loggerContext, "Decrypting data are: " + builder.decryptingData(cnx))
        }
    }
  }

}
