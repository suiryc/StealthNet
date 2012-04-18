package perso.stealthnet.network.protocol.exceptions

import perso.stealthnet.util.LoggingContext

class ProtocolException(
  msg: String = null,
  cause: Throwable = null,
  override val loggerContext: LoggingContext#LogContext = Nil
) extends Exception(msg, cause)
  with LoggingContext
