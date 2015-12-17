package stealthnet.scala.network.protocol.exceptions

import stealthnet.scala.util.log.LoggingContext

/**
 * Invalid data exception.
 */
class InvalidDataException(
  // scalastyle:off null
  msg: String = null,
  cause: Throwable = null,
  // scalastyle:on null
  override val loggerContext: LoggingContext#LogContext = Nil
) extends ProtocolException(msg, cause, loggerContext)
