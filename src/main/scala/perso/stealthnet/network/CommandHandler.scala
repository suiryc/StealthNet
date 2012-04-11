package perso.stealthnet.network

import java.io.InputStream
import java.nio.channels.ClosedChannelException
import javax.crypto.{Cipher, CipherOutputStream}
import com.weiglewilczek.slf4s.Logging
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
import perso.stealthnet.network.protocol.{
  Command,
  Constants,
  Encryption,
  ProtocolStream,
  RSAParametersClientCommand,
  RSAParametersServerCommand
}

class CommandHandler(val group: ChannelGroup) extends SimpleChannelHandler with Logging {

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val input: InputStream = e.getMessage.asInstanceOf[InputStream]
    val command = Command.read(input)

    if (command == null)
      return

    logger debug("Received command: " + command)

    /* XXX - handle command */
    /* XXX - do it here ? */
    command match {
      case c: RSAParametersServerCommand => e.getChannel.write(new RSAParametersClientCommand())
      case _ =>
        logger error("Unhandled command")
    }
  }

  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    /* XXX - check we can write and block or drop ? */
    val command: Command = e.getMessage.asInstanceOf[Command]
    val buf: ChannelBuffer = ChannelBuffers.dynamicBuffer(1024)
    val output = new ChannelBufferOutputStream(buf)

    logger debug("Sending command: " + command)

    val cipherLength = command.write(output)

    /* set the cipher-text command length */
    buf.setBytes(Constants.commandLengthOffset, ProtocolStream.convertShort(cipherLength))

    Channels.write(ctx, e.getFuture, buf)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    val cnx = StealthNetConnections.get(e.getChannel, create = false)

    e.getCause match {
      case e: ClosedChannelException =>
        /* disconnection was or will be notified */

      case _ =>
        val host: Any = if ((cnx != null) && (cnx.host != null))
            cnx.host
          else
            e.getChannel.getRemoteAddress

        if (host != null)
          logger debug ("Unexpected exception with host[" + host + "]!",  e.getCause)
        else
          logger debug ("Unexpected exception!",  e.getCause)
    }

    if (cnx != null)
      cnx.closing = true

    e.getChannel.close
  }

}
