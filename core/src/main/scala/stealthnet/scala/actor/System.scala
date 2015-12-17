package stealthnet.scala.actor

import akka.actor.{ActorRef, ActorSystem, Props}
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}
import suiryc.scala.akka.{Reaper, ShutdownReaper}

/**
 * Actors system helper.
 */
object System
  extends Logging
  with EmptyLoggingContext
{

  /** The actual system. */
  val system = ActorSystem("StealthNet")

  /** Our reaper. */
  private val reaper = system.actorOf(Props[ShutdownReaper], "Reaper")

  /** Let the reaper watch over a new actor. */
  def watch(actor: ActorRef) {
    reaper ! Reaper.WatchMe(actor)
  }

}
