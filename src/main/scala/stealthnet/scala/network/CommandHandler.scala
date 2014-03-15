package stealthnet.scala.network

import io.netty.buffer.{ByteBuf, ByteBufOutputStream, Unpooled}
import io.netty.channel.{
  ChannelHandlerContext,
  ChannelDuplexHandler,
  ChannelPromise
}
import io.netty.handler.timeout.{ReadTimeoutException, WriteTimeoutException}
import java.net.ConnectException
import java.nio.channels.ClosedChannelException
import stealthnet.scala.{Constants, Settings}
import stealthnet.scala.core.Core
import stealthnet.scala.network.connection.{
  StealthNetConnection,
  StealthNetConnectionsManager
}
import stealthnet.scala.network.protocol.{BitSize, ProtocolStream}
import stealthnet.scala.network.protocol.commands.Command
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}

/**
 * Inbound/outbound command handler.
 *
 * Handles received/to send commands.
 */
class CommandHandler
  extends ChannelDuplexHandler
  with Logging
  with EmptyLoggingContext
{

  /**
   * Handles received [[stealthnet.scala.network.protocol.commands.Command]].
   *
   * Actual processing is delegated to [[stealthnet.scala.core.Core]].`receivedCommand`.
   */
  override def channelRead(ctx: ChannelHandlerContext, msg: Any) {
    val command: Command = msg.asInstanceOf[Command]
    val cnx = StealthNetConnectionsManager.connection(ctx.channel)

    if (Settings.core.debugIOCommands)
      logger debug(cnx.loggerContext, s"Received command: $command")

    Core.receivedCommand(command, cnx)
  }

  /**
   * Handles [[stealthnet.scala.network.protocol.commands.Command]] to send.
   *
   * Writes the command into a new channel buffer propagated outbound.
   *
   * @see [[stealthnet.scala.network.protocol.commands.Command]].`write`
   * @todo queue messages (up to limit) until connection is established, then
   *   flush them
   * @todo check we can write and block or drop if not ?
   * @todo simplify by extending MessageToByteEncoder<Command> ?
   */
  override def write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
    val cnx = StealthNetConnectionsManager.connection(ctx.channel)
    if (!cnx.channel.isOpen || cnx.closing || Core.stopping)
      /* drop data */
      return

    val command: Command = msg.asInstanceOf[Command]
    val buf: ByteBuf = Unpooled.buffer(Constants.commandOutputBufferLength)
    val output = new ByteBufOutputStream(buf)

    if (Settings.core.debugIOCommands)
      logger debug(cnx.loggerContext, s"Sending command: $command")

    val cipherStart = buf.writerIndex + Constants.commandOffset
    val unencryptedLength = command.write(cnx, output)
    if (unencryptedLength > 0xFFFF) {
      logger error(cnx.loggerContext, "Command[%s] length exceeds capacity".format(command))
      return
    }
    val cipherLength = buf.writerIndex - cipherStart

    /* set the cipher-text command length */
    buf.setBytes(Constants.commandLengthOffset, ProtocolStream.convertInteger(cipherLength, BitSize.Short))

    ctx.write(output.buffer, promise);
    /* Note: don't forget to flush! */
    flush(ctx);
  }

  /**
   * Handles caught exceptions.
   *
   * Closes related channel.
   */
  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    val cnx = StealthNetConnectionsManager.getConnection(ctx.channel)
    val loggerContext = StealthNetConnection.loggerContext(cnx,  ctx.channel)

    logger trace(loggerContext, s"Caught exception: $cause")
    cause match {
      case e: ReadTimeoutException =>
        logger debug(loggerContext, "Read timeout")

      case e: WriteTimeoutException =>
        logger debug(loggerContext, "Write timeout")

      case e: ClosedChannelException =>
        /* disconnection was or will be notified */

      case e: ConnectException =>
        /* connection failure was notified */

      case _ =>
        if (cnx.map(!_.closing).getOrElse(true) && !Core.stopping)
          logger debug(loggerContext, "Unexpected exception!",  cause)
        /*else: nothing to say here */
    }

    cnx map(_.close()) getOrElse(ctx.channel.close())
  }

}
