package stealthnet.scala.util

trait Logging extends LoggingContext {

  protected[util] lazy val logger = Logger(this.getClass, loggerContext)

}

trait LoggingContext {

  /** Log context type. */
  type LogContext = List[(String, Any)]

  /** General log context. */
  protected def loggerContext: LogContext

}

trait EmptyLoggingContext extends LoggingContext {
  
  protected val loggerContext: LogContext = List.empty

}
