package perso.stealthnet.network

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
import org.jboss.netty.channel.group.ChannelGroup
import org.jboss.netty.handler.timeout.ReadTimeoutException
import perso.stealthnet.core.Core
import perso.stealthnet.network.protocol.{
  BitSize,
  Constants,
  ProtocolStream
}
import perso.stealthnet.network.protocol.commands.Command
import perso.stealthnet.util.{EmptyLoggingContext, Logging}

class CommandHandler(val group: ChannelGroup)
  extends SimpleChannelHandler
  with Logging
  with EmptyLoggingContext
{

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val command: Command = e.getMessage.asInstanceOf[Command]
    val cnx = StealthNetConnectionsManager.getConnection(e.getChannel)

    logger debug(cnx.loggerContext, "Received command: " + command)

    Core.processCommand(command, cnx)
  }

  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    if (!e.getChannel.isOpen)
      return

    /* XXX - queue messages (up to limit) until connection is established, then
     * flush them */
    /* XXX - check we can write and block or drop ? */
    val cnx = StealthNetConnectionsManager.getConnection(e.getChannel)
    val command: Command = e.getMessage.asInstanceOf[Command]
    val buf: ChannelBuffer = ChannelBuffers.dynamicBuffer(0)
    val output = new ChannelBufferOutputStream(buf)

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

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    val cnx = StealthNetConnectionsManager.getConnection(e.getChannel, false)

    logger debug(if (cnx != null) cnx.loggerContext else Nil, "Caught exception: " + e.getCause.getMessage())
    e.getCause match {
      case e: ReadTimeoutException =>
        logger debug(if (cnx != null) cnx.loggerContext else Nil, "Timeout waiting for remote host")

      case e: ClosedChannelException =>
        /* disconnection was or will be notified */

      case e: ConnectException =>
        /* connection failure was notified */

      case _ if (cnx.closing || Core.stopping) =>
        /* nothing to say here */

      case _ =>
        val loggerContext: List[(String, Any)] = if ((cnx != null) && (cnx.loggerContext != Nil))
            cnx.loggerContext
          else
            List("remote" -> e.getChannel.getRemoteAddress)

        logger debug(loggerContext, "Unexpected exception!",  e.getCause)
    }

    if (cnx != null)
      cnx.closing = true

    e.getChannel.close
  }

}
