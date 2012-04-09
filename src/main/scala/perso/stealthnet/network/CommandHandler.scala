package perso.stealthnet.network

import com.weiglewilczek.slf4s.Logging
import org.jboss.netty.buffer.{
  ChannelBuffer,
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
import java.nio.channels.ClosedChannelException
import perso.stealthnet.network.protocol.Command
import perso.stealthnet.network.protocol.ProtocolStream
import perso.stealthnet.network.protocol.Constants
import perso.stealthnet.network.protocol.Encryption
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import org.jboss.netty.buffer.ChannelBufferOutputStream
import java.io.InputStream

class CommandHandler(val group: ChannelGroup) extends SimpleChannelHandler with Logging {

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val input: InputStream = e.getMessage.asInstanceOf[InputStream]
    /* XXX - rebuild command */
    logger debug("Received command: "/* + command*/)
  }

  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    /* XXX - check we can write and block or drop ? */
    val command: Command = e.getMessage.asInstanceOf[Command]
    val buf: ChannelBuffer = ChannelBuffers.dynamicBuffer(1024)
    val output = new ChannelBufferOutputStream(buf)

    /* plain-text section */
    ProtocolStream.writeAscii(output, Constants.protocol)
    ProtocolStream.write(output, Encryption.id(command.encryption))
    output.flush()

    /* cipher-text section */
    /* XXX - get writing cipher of connection */
    val cipher: Cipher = null
    val cipherOutput = new CipherOutputStream(output, cipher)
    var cipherLength = 0
    ProtocolStream.write(cipherOutput, 0 shortValue)
    /* XXX - shouldn't there be the command code ? */
    for (argument <- command.arguments) {
      if (argument == null) {
        logger error("Missing command argument in " + command)
        return
      }

      cipherLength += ProtocolStream.write(cipherOutput, argument)
    }
    cipherOutput.flush()
    cipherOutput.close()

    if (cipherLength > 0xFFFF) {
      logger error("Command[%s] length exceeds capacity".format(command))
      return
    }

    /* set the cipher-text command length */
    buf.setBytes(Constants.commandLengthOffset, ProtocolStream.convertShort(cipherLength shortValue))

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
