package stealthnet.scala.network.connection

import scala.actors.Actor
import stealthnet.scala.core.Core
import stealthnet.scala.network.{StealthNetClient, WebCaches}
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}

/**
 * ''StealthNet'' client connections manager.
 *
 * Handles client-side connection actions.
 *
 * This manager is meant to be used by the more general
 * [[stealthnet.scala.network.connection.StealthNetConnectionsManager]].
 */
protected object StealthNetClientConnectionsManager
  extends Actor
  with Logging
  with EmptyLoggingContext
{

  /**
   * Actor message: requests new peer connection.
   *
   * This message is sent by the connections manager when a new connection is
   * needed.
   */
  case class RequestPeer()
  /**
   * Actor message: close client connection.
   *
   * This message is sent by the connections manager when a connection needs to
   * be closed.
   */
  case class CloseClient(cnx: StealthNetConnection)
  /**
   * Actor message: stop manager.
   *
   * This message is expected to be sent before closing the application.
   */
  case class Stop()

  def act() {
    loop {
      react {
        case RequestPeer() =>
          if (!Core.stopping) WebCaches.getPeer() match {
            case Some(peer) =>
              val client = new StealthNetClient(peer)
              client.start()
              StealthNetConnectionsManager !
                StealthNetConnectionsManager.RequestedPeer()

            case None =>
          }

        case CloseClient(cnx) =>
          cnx.client.get.stop()
          cnx.peer foreach { StealthNetConnectionsManager ! StealthNetConnectionsManager.RemovePeer(_) }

        case Stop() =>
          /* time to leave */
          logger debug("Stopped")
          exit()
      }
    }
  }

}
