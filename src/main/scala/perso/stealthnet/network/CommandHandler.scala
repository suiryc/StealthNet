package perso.stealthnet.network

import java.io.InputStream
import java.net.ConnectException
import java.nio.channels.ClosedChannelException
import javax.crypto.{Cipher, CipherOutputStream}
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
import perso.stealthnet.core.Core
import perso.stealthnet.core.util.{EmptyLoggingContext, Logging}
import perso.stealthnet.network.protocol.{
  Command,
  Constants,
  Encryption,
  ProtocolStream,
  RSAParametersClientCommand,
  RSAParametersServerCommand
}

class CommandHandler(val group: ChannelGroup)
  extends SimpleChannelHandler
  with Logging
  with EmptyLoggingContext
{

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val command: Command = e.getMessage.asInstanceOf[Command]

    logger debug(StealthNetConnections.get(e.getChannel).loggerContext, "Received command: " + command)

    Core.processCommand(command, e.getChannel)
  }

  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    /* XXX - check we can write and block or drop ? */
    val cnx = StealthNetConnections.get(e.getChannel)
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
    buf.setBytes(Constants.commandLengthOffset, ProtocolStream.convertShort(cipherLength))

    Channels.write(ctx, e.getFuture, output.buffer)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    val cnx = StealthNetConnections.get(e.getChannel, create = false)

    e.getCause match {
      case e: ClosedChannelException =>
        /* disconnection was or will be notified */

      case e: ConnectException =>
        /* connection failure was notified */

      case _ =>
        val host: Any = if ((cnx != null) && (cnx.host != null))
            cnx.host
          else
            e.getChannel.getRemoteAddress

        logger debug(if (cnx != null) cnx.loggerContext else Nil, "Unexpected exception!",  e.getCause)
    }

    if (cnx != null)
      cnx.closing = true

    e.getChannel.close
  }

}
