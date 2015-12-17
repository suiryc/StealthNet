package stealthnet.scala.network

import io.netty.channel.{
  ChannelHandlerContext,
  ChannelDuplexHandler,
  ChannelPromise
}
import stealthnet.scala.Settings
import stealthnet.scala.network.connection.StealthNetConnectionsManager
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
    val cnx = StealthNetConnectionsManager.connection(ctx.channel)
    val command: Command = msg.asInstanceOf[Command]

    if (Settings.core.debugIOCommands)
      logger.debug(cnx.loggerContext, s"Received command: $command")

    cnx.received(command)
  }

  /**
   * Handles [[stealthnet.scala.network.protocol.commands.Command]] to send.
   *
   * Propagates outbound.
   */
  override def write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
    val cnx = StealthNetConnectionsManager.connection(ctx.channel)
    val command: Command = msg.asInstanceOf[Command]

    if (Settings.core.debugIOCommands)
      logger.debug(cnx.loggerContext, s"Sending command: $command")

    ctx.write(command, promise)
    /* Note: don't forget to flush! */
    flush(ctx)
  }

}
