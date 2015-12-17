package stealthnet.scala.network

import io.netty.channel.{ChannelHandlerContext, ChannelDuplexHandler}
import io.netty.handler.timeout.{ReadTimeoutException, WriteTimeoutException}
import java.net.ConnectException
import java.nio.channels.ClosedChannelException
import stealthnet.scala.core.Core
import stealthnet.scala.network.connection.{
  StealthNetConnection,
  StealthNetConnectionsManager
}
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}

/**
 * Inbound/outbound exception handler.
 *
 * Handles caught exceptions.
 */
class ExceptionHandler
  extends ChannelDuplexHandler
  with Logging
  with EmptyLoggingContext
{

  /**
   * Handles caught exceptions.
   *
   * Closes related channel.
   */
  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    val cnx = StealthNetConnectionsManager.getConnection(ctx.channel)
    val loggerContext = StealthNetConnection.loggerContext(cnx,  ctx.channel)

    logger.trace(loggerContext, s"Caught exception: $cause")
    cause match {
      case e: ReadTimeoutException =>
        logger.debug(loggerContext, "Read timeout")

      case e: WriteTimeoutException =>
        logger.debug(loggerContext, "Write timeout")

      case e: ClosedChannelException =>
        /* disconnection was or will be notified */

      case e: ConnectException =>
        /* connection failure was notified */

      case _ =>
        if (cnx.map(!_.closing).getOrElse(true) && !Core.stopping)
          logger.debug(loggerContext, "Unexpected exception!",  cause)
        /*else: nothing to say here */
    }

    cnx.map(_.close()).getOrElse(ctx.channel.close())
  }

}
