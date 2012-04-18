package perso.stealthnet.network.protocol.exceptions

import perso.stealthnet.util.LoggingContext

class InvalidDataException(
  msg: String = null,
  cause: Throwable = null,
  override val loggerContext: LoggingContext#LogContext = Nil
) extends ProtocolException(msg, cause, loggerContext)
