package stealthnet.scala.network

import java.net.ConnectException
import java.nio.channels.ClosedChannelException
import org.jboss.netty.buffer.{
  ChannelBuffer,
  ChannelBufferOutputStream,
  ChannelBuffers
}
import org.jboss.netty.channel.{
  Channels,
  ChannelHandlerContext,
  ExceptionEvent,
  MessageEvent,
  SimpleChannelHandler
}
import org.jboss.netty.handler.timeout.{
  ReadTimeoutException,
  WriteTimeoutException
}
import stealthnet.scala.{Constants, Settings}
import stealthnet.scala.core.Core
import stealthnet.scala.network.protocol.{BitSize, ProtocolStream}
import stealthnet.scala.network.protocol.commands.Command
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}

/**
 * Upstream/downstream command handler.
 *
 * Handles received/to send commands.
 */
class CommandHandler
  extends SimpleChannelHandler
  with Logging
  with EmptyLoggingContext
{

  /**
   * Handles received [[stealthnet.scala.network.protocol.commands.Command]].
   *
   * Actual processing is delegated to [[stealthnet.scala.core.Core]].`receivedCommand`.
   */
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val command: Command = e.getMessage.asInstanceOf[Command]
    val cnx = StealthNetConnectionsManager.connection(e.getChannel)

    if (Settings.core.debugIOCommands)
      logger debug(cnx.loggerContext, "Received command: " + command)

    Core.receivedCommand(command, cnx)
  }

  /**
   * Handles [[stealthnet.scala.network.protocol.commands.Command]] to send.
   *
   * Writes the command into a new channel buffer propagated downstream.
   *
   * @see [[stealthnet.scala.network.protocol.commands.Command]].`write`
   * @todo queue messages (up to limit) until connection is established, then
   *   flush them
   * @todo check we can write and block or drop if not ?
   */
  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    val cnx = StealthNetConnectionsManager.connection(e.getChannel)
    if (!cnx.channel.isOpen || cnx.closing || Core.stopping)
      /* drop data */
      return

    val command: Command = e.getMessage.asInstanceOf[Command]
    val buf: ChannelBuffer = ChannelBuffers.dynamicBuffer(Constants.commandOutputBufferLength)
    val output = new ChannelBufferOutputStream(buf)

    if (Settings.core.debugIOCommands)
      logger debug(cnx.loggerContext, "Sending command: " + command)

    val cipherStart = buf.writerIndex + Constants.commandOffset
    val unencryptedLength = command.write(cnx, output)
    if (unencryptedLength > 0xFFFF) {
      logger error(cnx.loggerContext, "Command[%s] length exceeds capacity".format(command))
      return
    }
    val cipherLength = buf.writerIndex - cipherStart

    /* set the cipher-text command length */
    buf.setBytes(Constants.commandLengthOffset, ProtocolStream.convertInteger(cipherLength, BitSize.Short))

    Channels.write(ctx, e.getFuture, output.buffer)
  }

  /**
   * Handles caught exceptions.
   *
   * Closes related channel.
   */
  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    val cnx = StealthNetConnectionsManager.getConnection(e.getChannel)
    val loggerContext = StealthNetConnection.loggerContext(cnx,  e.getChannel)

    logger trace(loggerContext, "Caught exception: " + e.getCause.toString())
    e.getCause match {
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
          logger debug(loggerContext, "Unexpected exception!",  e.getCause)
        /*else: nothing to say here */
    }

    cnx map(_.close()) getOrElse(e.getChannel.close())
  }

}
