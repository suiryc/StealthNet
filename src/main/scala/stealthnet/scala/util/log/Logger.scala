/*
 * Copyright 2010-2011 Weigle Wilczek GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Modified version for StealthNet scala.
 * See https://github.com/weiglewilczek/slf4s for original version.
 */
package stealthnet.scala.util.log

import org.slf4j.{Logger => Slf4jLogger, LoggerFactory}

/**
* Factory for Loggers.
*/
object Logger {

  /**
   * Creates a Logger named corresponding to the given class.
   *
   * @param clazz class used for the Logger's name. Must not be `null`!
   */
  def apply(clazz: Class[_], context: LoggingContext#LogContext): Logger = {
    // scalastyle:off null
    require(clazz != null, "clazz must not be null!")
    // scalastyle:on null
    logger(LoggerFactory getLogger clazz, context)
  }

  /**
   * Creates a Logger with the given name.
   *
   * @param name the Logger's name. Must not be `null`!
   */
  def apply(name: String, context: LoggingContext#LogContext): Logger = {
    // scalastyle:off null
    require(name != null, "loggerName must not be null!")
    // scalastyle:on null
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

  /**
   * Gets message prefix from logging context data.
   *
   * Concatenates the logger logging context and the message logging context.
   *
   * @param msgContext message logging context
   */
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
   *
   * @param msg The message to be logged
   */
  def error(msg: => String) {
    if (slf4jLogger.isErrorEnabled) slf4jLogger.error(msgPrefix() + msg)
  }

  /**
   * Log a message with ERROR level.
   *
   * @param msgContext message logging context
   * @param msg The message to be logged
   */
  def error(msgContext: => LoggingContext#LogContext, msg: => String) {
    if (slf4jLogger.isErrorEnabled) slf4jLogger.error(msgPrefix(msgContext) + msg)
  }

  /**
   * Log a message with ERROR level.
   *
   * @param msg The message to be logged
   * @param t The Throwable to be logged
   */
  def error(msg: => String, t: Throwable) {
    if (slf4jLogger.isErrorEnabled) slf4jLogger.error(msgPrefix() + msg, t)
  }

  /**
   * Log a message with ERROR level.
   *
   * @param msgContext message logging context
   * @param msg The message to be logged
   * @param t The Throwable to be logged
   */
  def error(msgContext: => LoggingContext#LogContext, msg: => String, t: Throwable) {
    if (slf4jLogger.isErrorEnabled) slf4jLogger.error(msgPrefix(msgContext) + msg, t)
  }

  /**
   * Log a message with WARN level.
   *
   * @param msg The message to be logged
   */
  def warn(msg: => String) {
    if (slf4jLogger.isWarnEnabled) slf4jLogger.warn(msgPrefix() + msg)
  }

  /**
   * Log a message with WARN level.
   *
   * @param msgContext message logging context
   * @param msg The message to be logged
   */
  def warn(msgContext: => LoggingContext#LogContext, msg: => String) {
    if (slf4jLogger.isWarnEnabled) slf4jLogger.warn(msgPrefix(msgContext) + msg)
  }

  /**
   * Log a message with WARN level.
   *
   * @param msg The message to be logged
   * @param t The Throwable to be logged
   */
  def warn(msg: => String, t: Throwable) {
    if (slf4jLogger.isWarnEnabled) slf4jLogger.warn(msgPrefix() + msg, t)
  }

  /**
   * Log a message with WARN level.
   *
   * @param msgContext message logging context
   * @param msg The message to be logged
   * @param t The Throwable to be logged
   */
  def warn(msgContext: => LoggingContext#LogContext, msg: => String, t: Throwable) {
    if (slf4jLogger.isWarnEnabled) slf4jLogger.warn(msgPrefix(msgContext) + msg, t)
  }

  /**
   * Log a message with INFO level.
   *
   * @param msg The message to be logged
   */
  def info(msg: => String) {
    if (slf4jLogger.isInfoEnabled) slf4jLogger.info(msgPrefix() + msg)
  }

  /**
   * Log a message with INFO level.
   *
   * @param msgContext message logging context
   * @param msg The message to be logged
   */
  def info(msgContext: => LoggingContext#LogContext, msg: => String) {
    if (slf4jLogger.isInfoEnabled) slf4jLogger.info(msgPrefix(msgContext) + msg)
  }

  /**
   * Log a message with INFO level.
   *
   * @param msg The message to be logged
   * @param t The Throwable to be logged
   */
  def info(msg: => String, t: Throwable) {
    if (slf4jLogger.isInfoEnabled) slf4jLogger.info(msgPrefix() + msg, t)
  }

  /**
   * Log a message with INFO level.
   *
   * @param msgContext message logging context
   * @param msg The message to be logged
   * @param t The Throwable to be logged
   */
  def info(msgContext: => LoggingContext#LogContext, msg: => String, t: Throwable) {
    if (slf4jLogger.isInfoEnabled) slf4jLogger.info(msgPrefix(msgContext) + msg, t)
  }

  /**
   * Log a message with DEBUG level.
   *
   * @param msg The message to be logged
   */
  def debug(msg: => String) {
    if (slf4jLogger.isDebugEnabled) slf4jLogger.debug(msgPrefix() + msg)
  }

  /**
   * Log a message with DEBUG level.
   *
   * @param msgContext message logging context
   * @param msg The message to be logged
   */
  def debug(msgContext: => LoggingContext#LogContext, msg: => String) {
    if (slf4jLogger.isDebugEnabled) slf4jLogger.debug(msgPrefix(msgContext) + msg)
  }

  /**
   * Log a message with DEBUG level.
   *
   * @param msg The message to be logged
   * @param t The Throwable to be logged
   */
  def debug(msg: => String, t: Throwable) {
    if (slf4jLogger.isDebugEnabled) slf4jLogger.debug(msgPrefix() + msg, t)
  }

  /**
   * Log a message with DEBUG level.
   *
   * @param msgContext message logging context
   * @param msg The message to be logged
   * @param t The Throwable to be logged
   */
  def debug(msgContext: => LoggingContext#LogContext, msg: => String, t: Throwable) {
    if (slf4jLogger.isDebugEnabled) slf4jLogger.debug(msgPrefix(msgContext) + msg, t)
  }

  /**
   * Log a message with TRACE level.
   *
   * @param msg The message to be logged
   */
  def trace(msg: => String) {
    if (slf4jLogger.isTraceEnabled) slf4jLogger.trace(msgPrefix() + msg)
  }

  /**
   * Log a message with TRACE level.
   *
   * @param msgContext message logging context
   * @param msg The message to be logged
   */
  def trace(msgContext: => LoggingContext#LogContext, msg: => String) {
    if (slf4jLogger.isTraceEnabled) slf4jLogger.trace(msgPrefix(msgContext) + msg)
  }

  /**
   * Log a message with TRACE level.
   *
   * @param msg The message to be logged
   * @param t The Throwable to be logged
   */
  def trace(msg: => String, t: Throwable) {
    if (slf4jLogger.isTraceEnabled) slf4jLogger.trace(msgPrefix() + msg, t)
  }

  /**
   * Log a message with TRACE level.
   *
   * @param msgContext message logging context
   * @param msg The message to be logged
   * @param t The Throwable to be logged
   */
  def trace(msgContext: => LoggingContext#LogContext, msg: => String, t: Throwable) {
    if (slf4jLogger.isTraceEnabled) slf4jLogger.trace(msgPrefix(msgContext) + msg, t)
  }

  /**  Is the logger instance enabled for the ERROR level? */
  def isErrorEnabled = slf4jLogger.isErrorEnabled
  /**  Is the logger instance enabled for the WARN level? */
  def isWarnEnabled = slf4jLogger.isWarnEnabled
  /**  Is the logger instance enabled for the INFO level? */
  def isInfoEnabled = slf4jLogger.isInfoEnabled
  /**  Is the logger instance enabled for the DEBUG level? */
  def isDebugEnabled = slf4jLogger.isDebugEnabled
  /**  Is the logger instance enabled for the TRACE level? */
  def isTraceEnabled = slf4jLogger.isTraceEnabled

}

private final class DefaultLogger(
  override protected val slf4jLogger: Slf4jLogger,
  override protected val loggerContext: LoggingContext#LogContext
) extends Logger
