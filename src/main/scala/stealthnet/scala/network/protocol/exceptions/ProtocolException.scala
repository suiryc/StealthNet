package stealthnet.scala.network.protocol.exceptions

import stealthnet.scala.util.LoggingContext

class ProtocolException(
  msg: String = null,
  cause: Throwable = null,
  override val loggerContext: LoggingContext#LogContext = Nil
) extends Exception(msg, cause)
  with LoggingContext
