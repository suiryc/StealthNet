package stealthnet.scala.network.protocol.exceptions

import stealthnet.scala.util.log.LoggingContext

/**
 * Protocol exception.
 */
class ProtocolException(
  // scalastyle:off null
  msg: String = null,
  cause: Throwable = null,
  // scalastyle:on null
  override val loggerContext: LoggingContext#LogContext = Nil
) extends Exception(msg, cause)
  with LoggingContext
