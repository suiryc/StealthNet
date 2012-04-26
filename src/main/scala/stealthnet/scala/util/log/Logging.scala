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

/**
 * Provides a logger.
 */
trait Logging extends LoggingContext {

  /** Logger. */
  protected[util] lazy val logger = Logger(this.getClass, loggerContext)

}

/**
 * Provides a logging context.
 */
trait LoggingContext {

  /** Log context type. */
  type LogContext = List[(String, Any)]

  /** General log context. */
  protected def loggerContext: LogContext

}

/**
 * Provides a default empty logging context.
 */
trait EmptyLoggingContext extends LoggingContext {

  /** Empty logging context. */
  protected val loggerContext: LogContext = List.empty

}
