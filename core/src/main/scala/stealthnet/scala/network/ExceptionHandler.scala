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
  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    val cnx = StealthNetConnectionsManager.getConnection(ctx.channel)
    val loggerContext = StealthNetConnection.loggerContext(cnx,  ctx.channel)

    logger.trace(loggerContext, s"Caught exception: $cause")
    cause match {
      case _: ReadTimeoutException =>
        logger.debug(loggerContext, "Read timeout")

      case _: WriteTimeoutException =>
        logger.debug(loggerContext, "Write timeout")

      case _: ClosedChannelException =>
        /* disconnection was or will be notified */

      case _: ConnectException =>
        /* connection failure was notified */

      case _ =>
        if (cnx.forall(!_.closing) && !Core.stopping)
          logger.debug(loggerContext, "Unexpected exception!",  cause)
        /*else: nothing to say here */
    }

    cnx match {
      case Some(cnx) => cnx.close()
      case None      => ctx.channel.close()
    }
    ()
  }

}
