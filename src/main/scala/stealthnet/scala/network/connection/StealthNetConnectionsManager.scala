package stealthnet.scala.network.connection

import java.net.InetSocketAddress
import scala.actors.Actor
import scala.collection.mutable
import org.jboss.netty.channel.{Channel, ChannelLocal}
import stealthnet.scala.{Constants, Settings}
import stealthnet.scala.core.Core
import stealthnet.scala.network.WebCaches
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
 * The actual implementation uses actor messages to ensure thread-safety, and
 * provides methods to interact with the manager without worrying about it.
 *
 * Manager has to be `start`ed before being used, and `stop`ped before closing
 * the application.
 *
 * For each connection, workflow is:
 *   - for client-side only, before initiating connection: `addPeer`
 *     - if peer is not accepted, client does nothing
 *   - channel opening: `connection`
 *   - channel connected: `addConnection`
 *     - if connection is not accepted, the channel is closed
 *   - channel closed: `closedChannel`
 *
 * This manager delegates client connections requests to another manager
 * ([[stealthnet.scala.network.connection.StealthNetClientConnectionsManager]]).
 * Thus, when necessary a new peer connection request will be sent to the
 * client connections manager, which will return a response once a new client
 * has been instantiated.
 */
object StealthNetConnectionsManager
  extends Actor
  with Logging
  with EmptyLoggingContext
{

  /** Connected peers. */
  private val peers = mutable.Set[Peer]()

  /** Channel/connection association. */
  private val local: ChannelLocal[StealthNetConnection] =
    new ChannelLocal(false)

  /** Listeners. */
  private var listeners = List.empty[ConnectionListener]

  /** Whether a peer request is ongoing (inside this manager). */
  private var peerRequestOngoing  = false
  /** Whether a peer request was sent (to the client connections manager). */
  private var peerRequestSent = false

  /** Whether we are stopping. */
  private var stopping = false

  /**
   * Actor message: register a connection listener.
   *
   * Listeners are expected to register at application start.
   *
   * @note There is no need to unregister a listener, which is stopped with the
   *   application.
   */
  case class AddConnectionsListener(listener: ConnectionListener)
  /**
   * Actor message: requests new peer connection.
   *
   * This message is automatically sent when necessary. It resends itself until
   * connection number reaches the upper limit.
   */
  protected case class RequestPeer()
  /**
   * Actor message: remove remote peer.
   *
   * This message is sent by
   * [[stealthnet.scala.network.connection.StealthNetClientConnectionsManager]]
   * in response to a peer request. This is used to ensure we request one peer
   * at a time.
   */
  protected[connection] case class RequestedPeer()
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
   * Actor message: remove remote peer.
   *
   * This message is sent by
   * [[stealthnet.scala.network.connection.StealthNetClientConnectionsManager]]
   * in response to a connection closing.
   */
  protected[connection] case class RemovePeer(peer: Peer)
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
   * Actor message: get connection.
   *
   * The manager will reply with the
   * [[stealthnet.scala.network.connection.StealthNetConnection]] associated to
   * the given channel. If no connection was found, and creation was requested,
   * a new one is created.
   */
  case class GetConnection(channel: Channel, create: Boolean = true)
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
  case class ClosedChannel(channel: Channel)
  /**
   * Actor message: stop manager.
   *
   * This message is expected to be sent before closing the application.
   * It removes ourself from WebCaches if necessary, closes client connections
   * and stops the client connections manager. No reply is sent.
   */
  case class Stop()

  /**
   * Manages this actor messages.
   *
   * This method first starts the client connections manager and check whether
   * to add ourself to the WebCaches and request peer connections.
   *
   * @see [[stealthnet.scala.network.connection.StealthNetClientConnectionsManager]]
   * @see [[stealthnet.scala.network.connection.StealthNetConnectionsManager]].`checkConnectionsLimit`
   */
  // scalastyle:off method.length
  def act() {
    StealthNetClientConnectionsManager.start()
    /* Prime the pump. */
    checkConnectionsLimit()

    loop {
      react {
        case AddConnectionsListener(listener) =>
          listeners ::= listener

        case RequestPeer() =>
          peerRequestOngoing = false
          if (!upperLimitReached() || peerRequestSent) {
            /* one request at a time */
            if (!peerRequestSent) {
              StealthNetClientConnectionsManager !
                StealthNetClientConnectionsManager.RequestPeer()
              peerRequestSent = true
              /* check again later */
              Core.schedule(this ! RequestPeer(), Constants.peerRequestPeriod)
            }
            else {
              /* check again later */
              Core.schedule(this ! RequestPeer(), Constants.peerRequestCheckPeriod)
            }
            peerRequestOngoing = true
          }
          /* else: enough connections for now */

        case RequestedPeer() =>
          peerRequestSent = false

        case AddPeer(peer) =>
          reply(add(peer))

        case RemovePeer(peer) =>
          remove(peer)

        case AddConnection(cnx) =>
          reply(add(cnx))

        case GetConnection(channel, create) =>
          reply(get(channel, create))

        case ClosedChannel(channel) =>
          reply(closed(channel))

        case Stop() =>
          if (stopping || (peers.size == 0)) {
            /* time to leave */
            logger debug("Stopped")
            /* Note: we get back here when the last peer is unregistered, which
             * is when the last connection is closed. So we know we won't be
             * needed anymore
             */
            StealthNetClientConnectionsManager !
              StealthNetClientConnectionsManager.Stop()
            exit()
          }
          /* else: there still are connected peers, wait for the connections to
           * close */
          stopping = true
          WebCaches.removePeer()
          /* Note: as caller is expected to be stopping right now, WebCaches
           * won't allow re-adding ourself before we really do leave.
           */
      }
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
      logger debug("Refused connection with peer[" + peer + "]: stop pending")
      false
    }
    else if (upperLimitReached()) {
      logger debug("Refused connection with peer[" + peer + "]: limit reached")
      false
    }
    else if (peers.contains(peer)) {
      logger debug("Refused connection with peer[" + peer + "]: already connected")
      false
    }
    else {
      logger debug("Accepted connection with peer[" + peer + "]")
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
    val remoteAddress = cnx.channel.getRemoteAddress

    cnx.accepted = remoteAddress match {
      case socketAddress: InetSocketAddress =>
        /* Client-side connection peer was checked and added beforehand */
        if (cnx.isClient) {
          /* If we are stopping, don't accept this connection. */
          if (stopping) false else true
        }
        else {
          val address = socketAddress.getAddress.getHostAddress
          val peer = Peer(address, socketAddress.getPort)
          cnx.peer = Some(peer)
          add(peer)
        }

      case _ =>
        /* shall not happen */
        logger debug("Refused connection with endpoint[" + remoteAddress + "]: unhandled address type")
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
  protected def get(channel: Channel, create: Boolean): Option[StealthNetConnection] =
    Option(local.get(channel)) orElse {
      if (create) {
        /* initialize the connection object */
        val cnx = new StealthNetConnection(channel)
        local.set(channel, cnx)
        Some(cnx)
      }
      else
        None
    }

  /**
   * Indicates the given channel was closed.
   *
   * First unregisters the associated connection, then the remote peer.
   *
   * @param channel the channel which was closed
   * @return an option value containing the associated
   *   [[stealthnet.scala.network.connection.StealthNetConnection]], or `None`
   *   if none
   * @see [[stealthnet.scala.network.connection.StealthNetConnectionsManager]].`remove`
   */
  protected def closed(channel: Channel): Option[StealthNetConnection] = {
    val cnx = Option(local.remove(channel))

    logger debug(StealthNetConnection.loggerContext(cnx, channel), "Channel closed")

    cnx foreach { cnx =>
      cnx.peer foreach { remove(_) }
      listeners foreach { _ ! ConnectionListener.ClosedConnection(cnx) }
    }

    cnx
  }

  /**
   * Removes peer from connected peers.
   *
   * Once removed, the number of connections is checked. If the last peer is
   * removed while we are stopping, a last `Stop` message is sent to effectively
   * stop the manager.
   *
   * @param peer the peer to remove
   * @see [[stealthnet.scala.network.connection.StealthNetConnectionsManager]].`checkConnectionsLimit`
   */
  protected def remove(peer: Peer) {
    // scalastyle:off null
    assert(peer != null)
    // scalastyle:on null

    if (peers.remove(peer))
      logger trace("Removed peer[" + peer + "]")

    if (stopping && (peers.size == 0))
      /* time to really stop now */
      this ! Stop()
    else
      checkConnectionsLimit()
  }

  /**
   * Checks the current number of opened connections.
   *
   * If below the lower limit:
   *   - adds ourself to WebCaches
   *   - start requesting peer connections
   * Removes ourself from WebCaches if beyond the upper limit.
   *
   * @see [[stealthnet.scala.Settings]].`avgCnxCount`
   * @see [[stealthnet.scala.network.WebCaches]]
   */
  protected def checkConnectionsLimit() {
    if (peers.size < Settings.core.avgCnxCount) {
      WebCaches.addPeer()
      /* one request at a time */
      if (Settings.core.enableClientConnections && !peerRequestOngoing) {
        this ! RequestPeer()
        peerRequestOngoing = true
      }
    }
    else if (upperLimitReached())
      WebCaches.removePeer()
  }

  /** Gets whether the upper connection limit (`1.25 * average`) is reached. */
  protected def upperLimitReached() =
    if (peers.size >= 1.25 * Settings.core.avgCnxCount) true else false

  /**
   * Gets the connection associated to the given channel.
   *
   * If no connection is currently associated, a new one is created.
   *
   * @param channel the channel for which to get the associated connection
   * @return the associated connection
   */
  def connection(channel: Channel): StealthNetConnection =
    (this !? GetConnection(channel, true)).asInstanceOf[Option[StealthNetConnection]].get

  /**
   * Gets the connection associated to the given channel.
   *
   * @param channel the channel for which to get the associated connection
   * @return an option value containing the associated
   *   [[stealthnet.scala.network.connection.StealthNetConnection]], or `None`
   *   if none
   */
  def getConnection(channel: Channel): Option[StealthNetConnection] =
    (this !? GetConnection(channel, false)).asInstanceOf[Option[StealthNetConnection]]

  /**
   * Adds given connection.
   *
   * @param cnx the connection to add
   * @return `true` if connection was accepted (no limit reached),
   *   `false` otherwise
   */
  def addConnection(cnx: StealthNetConnection): Boolean =
    (this !? AddConnection(cnx)).asInstanceOf[Boolean]

  /**
   * Adds given peer.
   *
   * @param peer the peer to add
   * @return `true` if peer was accepted (no limit reached), `false` otherwise
   */
  def addPeer(peer: Peer): Boolean =
    (this !? AddPeer(peer)).asInstanceOf[Boolean]

  /**
   * Indicates the given channel was closed.
   *
   * @param channel the channel which was closed
   * @return an option value containing the associated
   *   [[stealthnet.scala.network.connection.StealthNetConnection]], or `None`
   *   if none
   */
  def closedChannel(channel: Channel) =
    (this !? ClosedChannel(channel)).asInstanceOf[Option[StealthNetConnection]]

  /** Stops the manager. */
  def stop() = if (!stopping) this ! Stop()

}