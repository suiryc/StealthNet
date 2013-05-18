package stealthnet.scala.network.connection

import akka.actor._
import akka.actor.ActorDSL._
import scala.concurrent._
import scala.concurrent.duration._
import stealthnet.scala.core.Core
import stealthnet.scala.network.{StealthNetClient, WebCachesManager}
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}

/**
 * ''StealthNet'' client connections manager.
 *
 * Handles client-side connection actions.
 *
 * This manager is meant to be used by the more general
 * [[stealthnet.scala.network.connection.StealthNetConnectionsManager]].
 */
protected object StealthNetClientConnectionsManager {

  /* XXX: migrate to aka
   *  - create actor class inside object: DONE
   *  - instantiate actor inside object: DONE
   *  - update API methods to use instantiated actor: DONE
   *  - shutdown system: DONE
   *  - use less ambiguous Stop message
   *  - use same system for all actors
   *    - use actorOf to create actor ?
   *    - address messages inside object to prevent ambigous names ?
   *  - use a shutdown pattern ? (http://letitcrash.com/post/30165507578/shutdown-patterns-in-akka-2)
   *  - use akka logging ?
   *  - cleanup
   */

  /**
   * Actor message: requests new peer connection.
   *
   * This message is sent by the connections manager when a new connection is
   * needed.
   */
  case object RequestPeer
  /**
   * Actor message: stop manager.
   *
   * This message is expected to be sent before closing the application.
   */
  case object Stop

  private class StealthNetClientConnectionsActor
    extends ActWithStash
    with Logging
    with EmptyLoggingContext
  {

    override def postStop() = context.system.shutdown()

    override def receive = {
      case RequestPeer =>
        if (!Core.stopping) WebCachesManager.getPeer() match {
          case Some(peer) =>
            val client = new StealthNetClient(peer)

            /* Note: it is important to notify the manager
             *   - after registering the remote peer
             *  and
             *   - before starting the client connection, or actually before
             *     the client can send a channel closing message
             */
            StealthNetConnectionsManager.actor !
              StealthNetConnectionsManager.RequestedPeer

            client.start()

          case None =>
            /* Notify the manager even upon failure to get a peer: it will
             * keep on requesting if necessary.
             */
            StealthNetConnectionsManager.actor !
              StealthNetConnectionsManager.RequestedPeer
        }

      case Stop =>
        /* time to leave */
        logger debug("Stopped")
        context.stop(self)
    }
  }

  private val system = ActorSystem("StealthNetClientConnections")
  lazy val actor = ActorDSL.actor(system)(new StealthNetClientConnectionsActor)

  /** Dummy method to start the manager. */
  def start() {
    actor
  }

}
