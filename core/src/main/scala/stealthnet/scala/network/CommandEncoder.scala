package stealthnet.scala.network

import io.netty.buffer.{ByteBuf, ByteBufOutputStream, Unpooled}
import io.netty.channel.{
  ChannelHandlerContext,
  ChannelOutboundHandlerAdapter,
  ChannelPromise
}
import stealthnet.scala.Constants
import stealthnet.scala.core.Core
import stealthnet.scala.network.connection.StealthNetConnectionsManager
import stealthnet.scala.network.protocol.{BitSize, ProtocolStream}
import stealthnet.scala.network.protocol.commands.Command
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}

/**
 * Outbound command encoder.
 */
class CommandEncoder
  extends ChannelOutboundHandlerAdapter
  with Logging
  with EmptyLoggingContext
{

  /**
   * Handles [[stealthnet.scala.network.protocol.commands.Command]] to send.
   *
   * Writes the command into a new channel buffer propagated outbound.
   *
   * @see [[stealthnet.scala.network.protocol.commands.Command]].`write`
   * @note we could simplify further by extending `MessageToByteEncoder<Command>`
   *   but lose some control over issues handling
   * @todo queue messages (up to limit) until connection is established, then
   *   flush them
   * @todo check we can write and block or drop if not ?
   */
  override def write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
    val cnx = StealthNetConnectionsManager.connection(ctx.channel)
    if (!cnx.channel.isOpen || cnx.closing || Core.stopping)
      /* drop data */
      return

    val command: Command = msg.asInstanceOf[Command]
    val buf: ByteBuf = Unpooled.buffer(Constants.commandOutputBufferLength)
    val output = new ByteBufOutputStream(buf)

    val cipherStart = buf.writerIndex + Constants.commandOffset
    val unencryptedLength = command.write(cnx, output)
    if (unencryptedLength > 0xFFFF) {
      logger.error(cnx.loggerContext, "Command[%s] length exceeds capacity".format(command))
      return
    }
    val cipherLength = buf.writerIndex - cipherStart

    /* set the cipher-text command length */
    buf.setBytes(Constants.commandLengthOffset, ProtocolStream.convertInteger(cipherLength.toLong, BitSize.Short))

    ctx.write(buf, promise)
    /* Note: don't forget to flush! */
    flush(ctx)
  }

}
