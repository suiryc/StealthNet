package stealthnet.scala.network.connection

import akka.actor._
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

  /* XXX: migrate to akka
   *  - use akka logging ?
   *  - refactor classes ?
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

  private class StealthNetClientConnectionsManagerActor
    extends Actor
    with Logging
    with EmptyLoggingContext
  {

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

  val actor = ActorDSL.actor(Core.actorSystem.system, "StealthNetClientConnectionsManager")(
    new StealthNetClientConnectionsManagerActor
  )
  Core.actorSystem.watch(actor)

  /** Dummy method to start the manager. */
  def start() { }

}
