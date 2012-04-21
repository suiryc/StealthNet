package stealthnet.scala.util

trait Logging extends LoggingContext {

  protected[util] lazy val logger = Logger(this.getClass, loggerContext)

}

trait LoggingContext {

  type LogContext = List[(String, Any)]

  protected def loggerContext: LogContext

}

trait EmptyLoggingContext extends LoggingContext {
  
  protected val loggerContext: LogContext = List.empty

}
