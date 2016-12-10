package stealthnet.scala.network

import akka.pattern.ask
import akka.util.Timeout
import io.netty.channel.{ChannelInboundHandlerAdapter, ChannelHandlerContext}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import stealthnet.scala.core.Core
import stealthnet.scala.network.connection.{
  StealthNetConnection,
  StealthNetConnectionsManager
}
import stealthnet.scala.network.protocol.commands.RSAParametersServerCommand
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}
import stealthnet.scala.util.netty.NettyDeferrer

/**
 * Inbound connection limiter.
 */
class ConnectionLimitHandler
  extends ChannelInboundHandlerAdapter
  with Logging
  with EmptyLoggingContext
{

  implicit private val timeout = Timeout(1.hour)

  /**
   * Handles channel connection event.
   *
   * Registers the [[stealthnet.scala.network.connection.StealthNetConnection]]
   * associated to the connected channel.
   * If connection limit is reached, the channel is closed.
   *
   * On server side, we start handshaking by sending our
   * [[stealthnet.scala.network.protocol.commands.RSAParametersServerCommand]].
   *
   * @note On server side, we would like to reject the connection as soon as
   *   possible (e.g. upon channel opening), but this does not seem to work as
   *   well as expected.
   *   So we just wait for the channel to be connected to cleanly close it.
   * @see [[stealthnet.scala.network.connection.StealthNetConnectionsManager]].`addConnection`
   */
  override def channelActive(ctx: ChannelHandlerContext) {
    val cnx = StealthNetConnectionsManager.connection(ctx.channel)

    /* XXX - is it ok to close the channel while super.channelActive was not called ? */
    NettyDeferrer.defer[Boolean](ctx, StealthNetConnectionsManager.actor ? StealthNetConnectionsManager.AddConnection(cnx)) {
      case Failure(_) =>
        cnx.close()

      case Success(allowed) =>
        if (!allowed) {
          cnx.close()
        }
        else {
          /* server starts handshake */
          if (!cnx.isClient)
            cnx.send(new RSAParametersServerCommand())

          super.channelActive(ctx)
        }
    }
  }

  /**
   * Handles channel closing event.
   *
   * Unregisters the [[stealthnet.scala.network.connection.StealthNetConnection]]
   * associated to the channel.
   *
   * @see [[stealthnet.scala.network.connection.StealthNetConnectionsManager]].`closedConnection`
   */
  override def channelInactive(ctx: ChannelHandlerContext) {
    val cnx = StealthNetConnectionsManager.getConnection(ctx.channel)
    val loggerContext = StealthNetConnection.loggerContext(cnx, ctx.channel)

    if (cnx.exists(cnx => !cnx.closing && !Core.stopping && (cnx.isClient || cnx.accepted)))
      logger.debug(loggerContext, "Remote host disconnected")
    else
      logger.debug(loggerContext, "Disconnected")

    /* Cleanup the connection */
    StealthNetConnectionsManager.closedChannel(ctx.channel)
    super.channelInactive(ctx)
  }

}
