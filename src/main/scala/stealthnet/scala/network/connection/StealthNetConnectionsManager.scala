package stealthnet.scala.network.connection

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import io.netty.channel.Channel
import io.netty.util.AttributeKey
import java.net.InetSocketAddress
import scala.concurrent._
import scala.collection.mutable
import stealthnet.scala.{Constants, Settings}
import stealthnet.scala.core.Core
import stealthnet.scala.network.{
  StealthNetClient,
  StealthNetServer,
  WebCachesManager
}
import stealthnet.scala.util.Peer
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}

/**
 * ''StealthNet'' connections manager.
 *
 * Peers or connections can be registered, up to the configured connection
 * limit. The manager automatically adds or removes ourself against the known
 * WebCaches, and requests peer connections, depending on the number of opened
 * (and accepted) connections:
 *   - lower limit is the configured average count
 *   - upper limit is `1.25 * average`
 *
 * Manager has to be `start`ed before being used, and `stop`ped before closing
 * the application.
 *
 * For each connection, workflow is:
 *   - for client-side only, before initiating connection: `AddPeer` message
 *     - if peer is not accepted, client does nothing
 *     - if connection fails: `RemovePeer` message
 *   - channel opening: `connection` method
 *   - channel connected: `AddConnection` message
 *     - if connection is not accepted, the channel is closed
 *   - channel closed: `closedChannel`
 *
 * Stopping the application core is a bit subtle: we have to keep resources
 * available until all connections are terminated, then cleanup can be done.
 * Thus the core only notifies the manager to stop: at this point, no new
 * peer/connection is allowed; once there is no more client request ongoing
 * (which could cause race conditions), we can close all connections; once the
 * last connection is terminated we can then call back the core to shutdown.
 */
object StealthNetConnectionsManager {

  /* XXX: use akka logging ? */

  /** Channel/connection association. */
  private val STEALTHNET_CONNECTION: AttributeKey[StealthNetConnection] =
    AttributeKey.valueOf("StealthNet.connection");

  /**
   * Actor message: register a connection listener.
   *
   * Listeners are expected to register at application start.
   *
   * @note There is no need to unregister a listener, which is stopped with the
   *   application.
   */
  protected case class AddConnectionsListener(listener: ActorRef)
  /**
   * Actor message: requests new peer connection.
   *
   * This message is automatically sent when necessary. It resends itself until
   * connection number reaches the upper limit.
   */
  protected case object RequestPeer
  /**
   * Actor message: add remote peer.
   *
   * The manager will reply with a Boolean indicating whether the peer was
   * accepted (no limit reached) or not.
   *
   * This message is expected to be used as a client-side operation before
   * attempting to connect to a given peer.
   */
  case class AddPeer(peer: Peer)
  /**
   * Actor message: add connection.
   *
   * The manager will reply with a Boolean indicating whether the connection was
   * accepted (no limit reached) or not.
   *
   * This message is expected to be used as both client-side and server-side
   * operation when a new connection is opened. On client side, the connection
   * is accepted (except if we are stopping) as it is expected the remote peer
   * was added beforehand.
   */
  case class AddConnection(cnx: StealthNetConnection)
  /**
   * Actor message: remove remote peer.
   *
   * This message is expected to be used as a client-side operation upon
   * connection failure.
   */
  case class RemovePeer(peer: Peer)
  /**
   * Actor message: connection closed.
   *
   * The manager will reply with an option value containing the
   * [[stealthnet.scala.network.connection.StealthNetConnection]] associated to
   * the given channel, or `None` if none was.
   *
   * This message is expected to be used as both client-side and server-side
   * operation when a connection is closed. This effectively unregisters the
   * associated connection against the manager.
   */
  protected case class ClosedChannel(channel: Channel)
  /**
   * Actor message: stop manager.
   *
   * This message is expected to be sent before closing the application.
   * It removes ourself from WebCaches if necessary, closes client connections
   * and stops the client connections manager. No reply is sent.
   */
  protected case object Stop

  private class StealthNetConnectionsManagerActor
    extends Actor
    with Logging
    with EmptyLoggingContext
  {

    /** Connected peers. */
    private val peers = mutable.Set[Peer]()

    /** Listeners. */
    private var listeners = List.empty[ActorRef]

    /** Whether a peer request is ongoing. */
    private var peerRequestOngoing  = false

    /** Whether we are stopping. */
    private var stopping = false

    override def preStart() {
      WebCachesManager.start()
      /* Prime the pump. */
      checkConnectionsLimit()
    }

    /**
     * Manages this actor messages.
     *
     * This method starts the WebCaches and client connections managers, and
     * checks whether to add ourself to the WebCaches and request peer
     * connections.
     *
     * @see [[stealthnet.scala.network.WebCachesManager]]
     * @see [[stealthnet.scala.network.connection.StealthNetConnectionsManager]].`checkConnectionsLimit`
     */
    // scalastyle:off method.length
    override def receive = {
      case AddConnectionsListener(listener) =>
        listeners ::= listener

      case RequestPeer =>
        peerRequestOngoing = false
        if (!stopping && !upperLimitReached()) {
          WebCachesManager.actor ! WebCachesManager.GetPeer
          /* check again later */
          Core.schedule(self ! RequestPeer,
            Constants.peerRequestPeriod)
          peerRequestOngoing = true
        }
        /* else: enough connections for now */

      case WebCachesManager.GotPeer(peer) =>
        val client = new StealthNetClient(peer)
        client.start()

      case AddPeer(peer) =>
        sender ! add(peer)

      case AddConnection(cnx) =>
        sender ! add(cnx)

      case RemovePeer(peer) =>
        remove(peer, true)

      case ClosedChannel(channel) =>
        closed(channel)

      case Stop =>
        if (peers.size == 0) {
          /* last connection terminated: time to leave */
          logger debug("Stopped")
          /* Note: we get back here when the last peer is unregistered, which
           * is when the last connection is closed. So we know we won't be
           * needed anymore
           */
          WebCachesManager.stop()
          Core.shutdown()
          context.stop(self)
        }
        else if (!stopping) {
          /* there still are connected peers, wait for the connections to
           * close */
          stopping = true
          WebCachesManager.actor ! WebCachesManager.RemovePeer
          /* Note: as caller is expected to be stopping right now, WebCaches
           * won't allow re-adding ourself before we really do leave.
           */

          /* Time to close connections */
          StealthNetServer.closeConnections()
        }
    }
    // scalastyle:on method.length

    /**
     * Adds given peer.
     *
     * If peer is accepted, the number of connections is checked.
     *
     * @param peer the peer to add
     * @return `true` if peer was accepted (no limit reached), `false` otherwise
     * @see [[stealthnet.scala.network.connection.StealthNetConnectionsManager]].`checkConnectionsLimit`
     */
    protected def add(peer: Peer): Boolean = {
      if (stopping) {
        logger debug s"Refused connection with peer[$peer]: stop pending"
        false
      }
      else if (upperLimitReached()) {
        logger debug s"Refused connection with peer[$peer]: limit reached"
        false
      }
      else if (peers.contains(peer)) {
        logger debug s"Refused connection with peer[$peer]: already connected"
        false
      }
      else {
        logger debug s"Accepted connection with peer[$peer]"
        peers.add(peer)
        checkConnectionsLimit()
        true
      }
    }

    /**
     * Adds given connection.
     *
     * @param cnx the connection to add
     * @return `true` if connection was accepted (no limit reached),
     *   `false` otherwise
     * @see [[stealthnet.scala.network.connection.StealthNetConnectionsManager]].`add(Peer)`
     */
    protected def add(cnx: StealthNetConnection): Boolean = {
      val remoteAddress = cnx.channel.remoteAddress

      cnx.accepted = remoteAddress match {
        case socketAddress: InetSocketAddress =>
          val peer = Peer(socketAddress)
          cnx.peer = Some(peer)

          /* Client-side connection peer was checked and added beforehand */
          if (cnx.isClient)
            /* If we are stopping, don't accept this connection. */
            if (stopping) false else true
          else
            add(peer)

        case _ =>
          /* shall not happen */
          logger debug s"Refused connection with endpoint[$remoteAddress]: unhandled address type"
          false
      }

      if (cnx.accepted)
        listeners foreach { _ ! ConnectionListener.NewConnection(cnx) }

      /* Note: if connection is not accepted, caller is expected to close the
       * channel, which will trigger a ClosedChannel message: this is when we do
       * unregister the connection.
       */
      cnx.accepted
    }

    /**
     * Indicates the given channel was closed.
     *
     * First unregisters the associated connection, then the remote peer.
     *
     * @param channel the channel which was closed
     * @see [[stealthnet.scala.network.connection.StealthNetConnectionsManager]].`remove`
     */
    protected def closed(channel: Channel) {
      val cnx = Option(channel.attr[StealthNetConnection](STEALTHNET_CONNECTION).getAndRemove())

      logger debug(StealthNetConnection.loggerContext(cnx, channel), "Channel closed")

      cnx foreach { cnx =>
        cnx.peer foreach { remove(_, cnx.accepted) }
        listeners foreach { _ ! ConnectionListener.ClosedConnection(cnx) }
      }
    }

    /**
     * Removes peer from connected peers.
     *
     * Once removed, the number of connections is checked. If the last peer is
     * removed while we are stopping, a last `Stop` message is sent to effectively
     * stop the manager.
     *
     * @param peer the peer to remove
     * @param accepted whether peer had been accepted
     * @see [[stealthnet.scala.network.connection.StealthNetConnectionsManager]].`checkConnectionsLimit`
     */
    protected def remove(peer: Peer, accepted: Boolean) {
      // scalastyle:off null
      assert(peer != null)
      // scalastyle:on null

      if (peers.remove(peer))
        logger trace s"Removed peer[$peer]"
      else if (accepted)
        logger debug s"Peer[$peer] was unknown"

      if (!stopping)
        checkConnectionsLimit()
      else if (peers.size == 0)
        /* may be time to really stop now */
        self ! Stop
    }

    /**
     * Checks the current number of opened connections.
     *
     * If not stopping and below the lower limit:
     *   - adds ourself to WebCaches
     *   - start requesting peer connections
     * Removes ourself from WebCaches if beyond the upper limit.
     *
     * @see [[stealthnet.scala.Settings]].`avgCnxCount`
     * @see [[stealthnet.scala.network.WebCachesManager]]
     */
    protected def checkConnectionsLimit() {
      if (!stopping && (peers.size < Settings.core.avgCnxCount)) {
        WebCachesManager.actor ! WebCachesManager.AddPeer
        /* one request at a time */
        if (Settings.core.enableClientConnections && !peerRequestOngoing) {
          self ! RequestPeer
          peerRequestOngoing = true
        }
      }
      else if (upperLimitReached())
        WebCachesManager.actor ! WebCachesManager.RemovePeer
    }

    /** Gets whether the upper connection limit (`1.25 * average`) is reached. */
    protected def upperLimitReached() =
      if (peers.size >= 1.25 * Settings.core.avgCnxCount) true else false

  }

  val actor = Core.actorSystem.system.actorOf(Props[StealthNetConnectionsManagerActor], "StealthNetConnectionsManager")
  Core.actorSystem.watch(actor)

  /**
   * Gets the connection associated to the given channel.
   *
   * @param channel the channel for which to get the associated connection
   * @param create whether to create a new
   *   [[stealthnet.scala.network.connection.StealthNetConnection]] if none is
   *   currently associated to the channel
   * @return an option value containing the associated
   *   [[stealthnet.scala.network.connection.StealthNetConnection]], or `None`
   *   if none
   */
  protected def get(channel: Channel, create: Boolean): Option[StealthNetConnection] = {
    val attr = channel.attr[StealthNetConnection](STEALTHNET_CONNECTION)
    if (create) {
      attr.setIfAbsent {
        /* initialize the connection object */
        new StealthNetConnection(channel)
      }
    }
    Option(attr.get)
  }

  /**
   * Gets the connection associated to the given channel.
   *
   * If no connection is currently associated, a new one is created.
   *
   * @param channel the channel for which to get the associated connection
   * @return the associated connection
   */
  def connection(channel: Channel): StealthNetConnection =
    get(channel, true).get

  /**
   * Gets the connection associated to the given channel.
   *
   * @param channel the channel for which to get the associated connection
   * @return an option value containing the associated
   *   [[stealthnet.scala.network.connection.StealthNetConnection]], or `None`
   *   if none
   */
  def getConnection(channel: Channel): Option[StealthNetConnection] =
    get(channel, false)

  /**
   * Indicates the given channel was closed.
   *
   * @param channel the channel which was closed
   */
  def closedChannel(channel: Channel) =
    actor ! ClosedChannel(channel)

  /** Adds a new connections listener. */
  def addConnectionsListener(listener: ActorRef) =
    actor ! AddConnectionsListener(listener)

  /**
   * Stops the manager.
   *
   * Performs the following actions:
   *   - removes ourself from WebCaches if necessary
   *     - for safety this is also done when this manager finally stops, but it
   *       is better to do it as soon as possible
   *   - stops this manager
   *
   * @see [[stealthnet.scala.network.WebCachesManager]]
   */
  def stop() {
    WebCachesManager.actor ! WebCachesManager.RemovePeer
    actor ! Stop
  }

  /**
   * Starts the manager.
   *
   * Shall be called only once.
   *
   * Performs the following actions:
   *   - (implicitely) starts this manager
   *   - refreshes the WebCaches list
   *
   * @see [[stealthnet.scala.network.WebCachesManager]]
   */
  def start() {
    WebCachesManager.actor ! WebCachesManager.Refresh
  }

}
