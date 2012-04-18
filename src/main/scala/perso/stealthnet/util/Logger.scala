package perso.stealthnet.util

import org.slf4j.{Logger => Slf4jLogger, LoggerFactory}

/**
* Factory for Loggers.
*/
object Logger {

  /**
   * Creates a Logger named corresponding to the given class.
   * @param clazz Class used for the Logger's name. Must not be null!
   */
  def apply(clazz: Class[_], context: LoggingContext#LogContext): Logger = {
    require(clazz != null, "clazz must not be null!")
    logger(LoggerFactory getLogger clazz, context)
  }

  /**
   * Creates a Logger with the given name.
   * @param name The Logger's name. Must not be null!
   */
  def apply(name: String, context: LoggingContext#LogContext): Logger = {
    require(name != null, "loggerName must not be null!")
    logger(LoggerFactory getLogger name, context)
  }

  private def logger(slf4jLogger: Slf4jLogger, context: LoggingContext#LogContext): Logger =
    new DefaultLogger(slf4jLogger, context)

}

/**
* Thin wrapper for SLF4J making use of by-name parameters to improve performance.
*/
trait Logger extends LoggingContext {

  /**
   * The name of this Logger.
   */
  lazy val name = slf4jLogger.getName

  /**
   * The wrapped SLF4J Logger.
   */
  protected val slf4jLogger: Slf4jLogger

  protected def msgPrefix(msgContext: LoggingContext#LogContext = Nil): String =
    (loggerContext ::: msgContext) match {
      case Nil =>
        ""

      case list =>
        list.map(tuple =>
          tuple._1 + "=" + tuple._2).mkString("[", "][", "] ")
    }

  /**
   * Log a message with ERROR level.
   * @param msg The message to be logged
   */
  def error(msg: => String) {
    if (slf4jLogger.isErrorEnabled) slf4jLogger.error(msgPrefix() + msg)
  }

  def error(msgContext: => LoggingContext#LogContext, msg: => String) {
    if (slf4jLogger.isErrorEnabled) slf4jLogger.error(msgPrefix(msgContext) + msg)
  }

  /**
   * Log a message with ERROR level.
   * @param msg The message to be logged
   * @param t The Throwable to be logged
   */
  def error(msg: => String, t: Throwable) {
    if (slf4jLogger.isErrorEnabled) slf4jLogger.error(msgPrefix() + msg, t)
  }

  def error(msgContext: => LoggingContext#LogContext, msg: => String, t: Throwable) {
    if (slf4jLogger.isErrorEnabled) slf4jLogger.error(msgPrefix(msgContext) + msg, t)
  }

  /**
   * Log a message with WARN level.
   * @param msg The message to be logged
   */
  def warn(msg: => String) {
    if (slf4jLogger.isWarnEnabled) slf4jLogger.warn(msgPrefix() + msg)
  }

  def warn(msgContext: => LoggingContext#LogContext, msg: => String) {
    if (slf4jLogger.isWarnEnabled) slf4jLogger.warn(msgPrefix(msgContext) + msg)
  }

  /**
   * Log a message with WARN level.
   * @param msg The message to be logged
   * @param t The Throwable to be logged
   */
  def warn(msg: => String, t: Throwable) {
    if (slf4jLogger.isWarnEnabled) slf4jLogger.warn(msgPrefix() + msg, t)
  }

  def warn(msgContext: => LoggingContext#LogContext, msg: => String, t: Throwable) {
    if (slf4jLogger.isWarnEnabled) slf4jLogger.warn(msgPrefix(msgContext) + msg, t)
  }

  /**
   * Log a message with INFO level.
   * @param msg The message to be logged
   */
  def info(msg: => String) {
    if (slf4jLogger.isInfoEnabled) slf4jLogger.info(msgPrefix() + msg)
  }

  def info(msgContext: => LoggingContext#LogContext, msg: => String) {
    if (slf4jLogger.isInfoEnabled) slf4jLogger.info(msgPrefix(msgContext) + msg)
  }

  /**
   * Log a message with INFO level.
   * @param msg The message to be logged
   * @param t The Throwable to be logged
   */
  def info(msg: => String, t: Throwable) {
    if (slf4jLogger.isInfoEnabled) slf4jLogger.info(msgPrefix() + msg, t)
  }

  def info(msgContext: => LoggingContext#LogContext, msg: => String, t: Throwable) {
    if (slf4jLogger.isInfoEnabled) slf4jLogger.info(msgPrefix(msgContext) + msg, t)
  }

  /**
   * Log a message with DEBUG level.
   * @param msg The message to be logged
   */
  def debug(msg: => String) {
    if (slf4jLogger.isDebugEnabled) slf4jLogger.debug(msgPrefix() + msg)
  }

  def debug(msgContext: => LoggingContext#LogContext, msg: => String) {
    if (slf4jLogger.isDebugEnabled) slf4jLogger.debug(msgPrefix(msgContext) + msg)
  }

  /**
   * Log a message with DEBUG level.
   * @param msg The message to be logged
   * @param t The Throwable to be logged
   */
  def debug(msg: => String, t: Throwable) {
    if (slf4jLogger.isDebugEnabled) slf4jLogger.debug(msgPrefix() + msg, t)
  }

  def debug(msgContext: => LoggingContext#LogContext, msg: => String, t: Throwable) {
    if (slf4jLogger.isDebugEnabled) slf4jLogger.debug(msgPrefix(msgContext) + msg, t)
  }

  /**
   * Log a message with TRACE level.
   * @param msg The message to be logged
   */
  def trace(msg: => String) {
    if (slf4jLogger.isTraceEnabled) slf4jLogger.trace(msgPrefix() + msg)
  }

  def trace(msgContext: => LoggingContext#LogContext, msg: => String) {
    if (slf4jLogger.isTraceEnabled) slf4jLogger.trace(msgPrefix(msgContext) + msg)
  }

  /**
   * Log a message with TRACE level.
   * @param msg The message to be logged
   * @param t The Throwable to be logged
   */
  def trace(msg: => String, t: Throwable) {
    if (slf4jLogger.isTraceEnabled) slf4jLogger.trace(msgPrefix() + msg, t)
  }

  def trace(msgContext: => LoggingContext#LogContext, msg: => String, t: Throwable) {
    if (slf4jLogger.isTraceEnabled) slf4jLogger.trace(msgPrefix(msgContext) + msg, t)
  }

}

private final class DefaultLogger(
  override protected val slf4jLogger: Slf4jLogger,
  override protected val loggerContext: LoggingContext#LogContext
) extends Logger
