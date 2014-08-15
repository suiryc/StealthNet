package stealthnet.scala.actor

import akka.actor.{Actor, ActorRef, Terminated}
import scala.collection.mutable.ArrayBuffer
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}

/**
 * Reaper companion object.
 */
object Reaper {

  /** Actor message: register an Actor for watching. */
  case class WatchMe(ref: ActorRef)

}

/**
 * Actors system reaper.
 *
 * The reaper watch over a list of registered actors, and call `allSoulsReaped`
 * once all actors terminated.
 *
 * @see [[http://letitcrash.com/post/30165507578/shutdown-patterns-in-akka-2]]
 */
abstract class Reaper
  extends Actor
  with Logging
  with EmptyLoggingContext
{
  import Reaper._

  /** Watched actors. */
  protected val watched = ArrayBuffer.empty[ActorRef]

  /**
   * Subclasses need to implement this method. It's the hook that's called when
   * everything's dead.
   */
  protected def allSoulsReaped(): Unit

  /** Watch and check for termination. */
  final override def receive = {
    case WatchMe(ref) =>
      logger.debug(s"Watching $ref")
      context.watch(ref)
      watched += ref

    case Terminated(ref) =>
      logger.debug(s"$ref terminated")
      watched -= ref
      if (watched.isEmpty) {
        logger.debug("All souls reaped")
        allSoulsReaped()
      }
  }

}

/** Simple reaper that shutdowns the system once finished. */
class ProductionReaper extends Reaper {

  /** Shutdown */
  override protected def allSoulsReaped(): Unit = context.system.shutdown()

}
