package stealthnet.scala.network

import java.net.InetSocketAddress
import java.security.interfaces.RSAPublicKey
import java.util.TimerTask
import scala.actors.Actor
import scala.collection.mutable
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelLocal
import org.jboss.netty.channel.group.ChannelGroup
import org.bouncycastle.crypto.BufferedBlockCipher
import stealthnet.scala.Config
import stealthnet.scala.core.Core
import stealthnet.scala.cryptography.{Ciphers, RijndaelParameters}
import stealthnet.scala.util.Peer
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging, LoggingContext}

/**
 * Bare ''StealthNet'' connection parameters.
 *
 * @note When setting the local or remote ''Rijndael'' parameters, the
 *   corresponding encrypter/decrypter is created as a side effect. It is to be
 *   reseted and used when necessary, instead of creating a new one each time.
 *   This is not done for ''RSA'' which is used only once during handshaking.
 */
class StealthNetConnectionParameters(
  /** Channel group to which register an opened channel. */
  var group: ChannelGroup = null,
  /** Client object, `null` on server side. */
  var client: StealthNetClient = null,
  /** Remote peer. */
  var peer: Peer = null
) extends LoggingContext
{

  def loggerContext = {
    if (peer != null)
      List("peer" -> peer)
    else
      List.empty
  }

  /** Whether connection was accepted (no limit reached). */
  var accepted: Boolean = true
  /** Whether connection was established (handshake successful). */
  var established: Boolean = false
  /** Whether connection is being closed. */
  var closing: Boolean = false
  /** Remote ''RSA'' public key to encrypt data. */
  var remoteRSAKey: RSAPublicKey = null

  /** Local ''Rijndael'' parameters to encrypt data. */
  private[this] var localRijndaelParams: RijndaelParameters = null
  /** ''Rijndael'' encrypter. */
  var rijndaelEncrypter: BufferedBlockCipher = null

  /** Gets local ''Rijndael'' parameters to encrypt data. */
  def localRijndaelParameters: RijndaelParameters = localRijndaelParams

  /**
   * Sets local ''Rijndael'' parameters to encrypt data.
   *
   * As a side effect, also creates the related encrypter.
   */
  def localRijndaelParameters_=(params: RijndaelParameters) {
    localRijndaelParams = params
    rijndaelEncrypter = Ciphers.rijndaelEncrypter(localRijndaelParams)
  }

  /** Remote ''Rijndael'' parameters to decrypt data. */
  private[this] var remoteRijndaelParams: RijndaelParameters = null
  /** ''Rijndael'' decrypter. */
  var rijndaelDecrypter: BufferedBlockCipher = null

  /** Gets remote ''Rijndael'' parameters to decrypt data. */
  def remoteRijndaelParameters: RijndaelParameters = remoteRijndaelParams

  /**
   * Sets remote ''Rijndael'' parameters to decrypt data.
   *
   * As a side effect, also creates the related decrypter.
   */
  def remoteRijndaelParameters_=(params: RijndaelParameters) {
    remoteRijndaelParams = params
    rijndaelDecrypter = Ciphers.rijndaelDecrypter(remoteRijndaelParams)
  }

  /** Gets whether this connection is a client one. */
  def isClient() = if (client != null) true else false

}

/**
 * ''StealthNet'' connection associated to a channel.
 */
class StealthNetConnection protected[network] (val channel: Channel)
  extends StealthNetConnectionParameters()
{

  assert(channel != null)

  /** Closes the connection channel. */
  def close() {
    closing = true
    channel.close()
  }

}

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
 *   - channel opening: `getConnection` (creation mode)
 *   - channel connected: `addConnection`
 *     - if connection is not accepted, the channel is closed
 *   - channel closed: `closedChannel`
 *
 * This manager delegates client connections actions to another manager
 * ([[stealthnet.scala.network.StealthNetClientConnectionsManager]]).
 * This is actually necessary due to the way the actor-based implementation is
 * used (synchronous message/reply), to prevent deadlocks (closing a client
 * connection will call back the connections manager).
 * Thus, when necessary a new peer connection request will be sent to the
 * client connections manager, which will return a response once a new client
 * has been instantiated. Similarly, upon receiving a channel closing message on
 * a client connection, a connection closing request will be sent to the client
 * manager, which will send back a peer removal request upon completion.
 */
object StealthNetConnectionsManager
  extends Actor
  with Logging
  with EmptyLoggingContext
{

  /** Connected hosts. */
  private val hosts = mutable.Set[String]()

  /** Channel/connection association. */
  private val local: ChannelLocal[StealthNetConnection] =
    new ChannelLocal(false)

  /** Whether a peer request is ongoing (inside this manager). */
  private var peerRequestOngoing  = false
  /** Whether a peer request was sent (to the client connections manager). */
  private var peerRequestSent = false

  /** Whether we are stopping. */
  private var stopping = false

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
   * [[stealthnet.scala.network.StealthNetClientConnectionsManager]] in response
   * to a peer request. This is used to ensure we request one peer at a time.
   */
  protected[network] case class RequestedPeer()
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
   * [[stealthnet.scala.network.StealthNetClientConnectionsManager]] in response
   * to a connection closing.
   */
  protected[network] case class RemovePeer(peer: Peer)
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
   * [[stealthnet.scala.network.StealthNetConnection]] associated to the given
   * channel. If no connection was found, and creation was requested, a new one
   * is created.
   */
  case class GetConnection(channel: Channel, create: Boolean = true)
  /**
   * Actor message: connection closed.
   *
   * The manager will reply with an option value containing the
   * [[stealthnet.scala.network.StealthNetConnection]] associated to the given
   * channel, or `None` if none was.
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
   * @see [[stealthnet.scala.network.StealthNetClientConnectionsManager]]
   * @see [[stealthnet.scala.network.StealthNetConnectionsManager]].`checkConnectionsLimit`
   */
  def act() {
    StealthNetClientConnectionsManager.start()
    /* Prime the pump. */
    checkConnectionsLimit()

    loop {
      react {
        case RequestPeer() =>
          peerRequestOngoing = false
          if (!upperLimitReached() || peerRequestSent) {
            /* one request at a time */
            if (!peerRequestSent) {
              StealthNetClientConnectionsManager !
                StealthNetClientConnectionsManager.RequestPeer()
              peerRequestSent = true
              /* check again in 5s */
              Core.schedule(this ! RequestPeer(), 5000)
            }
            else {
              /* check again in 2s */
              Core.schedule(this ! RequestPeer(), 2000)
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
          if (stopping || (hosts.size == 0)) {
            /* time to leave */
            logger debug("Stopping")
            /* Note: we get back here when the last host is unregistered, which
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

  /**
   * Adds given peer.
   *
   * If peer is accepted, the number of connections is checked.
   *
   * @param peer the peer to add
   * @return `true` if peer was accepted (no limit reached), `false` otherwise
   * @see [[stealthnet.scala.network.StealthNetConnectionsManager]].`checkConnectionsLimit`
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
    else if (hosts.contains(peer.host)) {
      logger debug("Refused connection with peer[" + peer + "]: host already connected")
      false
    }
    else {
      logger debug("Accepted connection with peer[" + peer + "]")
      hosts.add(peer.host)
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
   * @see [[stealthnet.scala.network.StealthNetConnectionsManager]].`add(Peer)`
   */
  protected def add(cnx: StealthNetConnection): Boolean = {
    val remoteAddress = cnx.channel.getRemoteAddress

    cnx.accepted = remoteAddress match {
      case socketAddress: InetSocketAddress =>
        /* Client-side connection host was checked and added beforehand */
        if (cnx.isClient) {
          /* If we are stopping, don't accept this connection. */
          if (stopping) false else true
        }
        else {
          val address = socketAddress.getAddress.getHostAddress
          cnx.peer = Peer(address, socketAddress.getPort)
          add(cnx.peer)
        }

      case _ =>
        /* shall not happen */
        logger debug("Refused connection with endpoint[" + remoteAddress + "]: unhandled address type")
        false
    }

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
   *   [[stealthnet.scala.network.StealthNetConnection]] if none is currently
   *   associated to the channel
   * @return an option value containing the associated
   *   [[stealthnet.scala.network.StealthNetConnection]], or `None` if none
   */
  protected def get(channel: Channel, create: Boolean): Option[StealthNetConnection] = {
    var cnx = local.get(channel)

    if ((cnx == null) && create) {
      /* initialize the connection object */
      cnx = new StealthNetConnection(channel)
      local.set(channel, cnx)
    }

    if (cnx != null)
      Some(cnx)
    else
      None
  }

  /**
   * Indicates the given channel was closed.
   *
   * First unregisters the associated connection. Then:
   *   - on client side, request connection closing to
   *     [[stealthnet.scala.network.StealthNetClientConnectionsManager]]
   *   - on server side unregisters the remote peer
   *
   * @param channel the channel which was closed
   * @return an option value containing the associated
   *   [[stealthnet.scala.network.StealthNetConnection]], or `None` if none
   * @see [[stealthnet.scala.network.StealthNetConnectionsManager]].`remove`
   * @see [[stealthnet.scala.network.StealthNetClientConnectionsManager]]
   */
  protected def closed(channel: Channel): Option[StealthNetConnection] = {
    val cnx = local.remove(channel)

    logger debug(cnx.loggerContext, "Channel closed")

    if (cnx != null) {
      if (cnx.isClient())
        StealthNetClientConnectionsManager !
          StealthNetClientConnectionsManager.CloseClient(cnx)
      else
        remove(cnx.peer)

      Some(cnx)
    }
    else
      None
  }

  /**
   * Removes peer from connected peers.
   *
   * Once removed, the number of connections is checked. If the last peer is
   * removed while we are stopping, a last `Stop` message is sent to effectively
   * stop the manager.
   *
   * @param peer the peer to remove
   * @see [[stealthnet.scala.network.StealthNetConnectionsManager]].`checkConnectionsLimit`
   */
  protected def remove(peer: Peer) {
    if (peer == null)
      return

    hosts.remove(peer.host)
    if (stopping && (hosts.size == 0))
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
   * @see [[stealthnet.scala.Config]].`avgCnxCount`
   * @see [[stealthnet.scala.network.WebCaches]]
   */
  protected def checkConnectionsLimit() {
    if (hosts.size < Config.avgCnxCount) {
      WebCaches.addPeer()
      /* one request at a time */
      if (Config.enableClientConnections && !peerRequestOngoing) {
        this ! RequestPeer()
        peerRequestOngoing = true
      }
    }
    else if (upperLimitReached())
      WebCaches.removePeer()
  }

  /** Gets whether the upper connection limit (`1.25 * average`) is reached. */
  protected def upperLimitReached() =
    if (hosts.size >= 1.25 * Config.avgCnxCount) true else false

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
   *   [[stealthnet.scala.network.StealthNetConnection]], or `None` if none
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
   *   [[stealthnet.scala.network.StealthNetConnection]], or `None` if none
   */
  def closedChannel(channel: Channel) =
    (this !? ClosedChannel(channel)).asInstanceOf[Option[StealthNetConnection]]

  /** Stops the manager. */
  def stop() = if (!stopping) this ! Stop()

}

/**
 * ''StealthNet'' client connections manager.
 *
 * Handles client-side connection actions.
 *
 * This manager is meant to be used by the more general
 * [[stealthnet.scala.network.StealthNetConnectionsManager]].
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
          cnx.client.stop()
          StealthNetConnectionsManager !
            StealthNetConnectionsManager.RemovePeer(cnx.peer)

        case Stop() =>
          /* time to leave */
          logger debug("Stopping")
          exit()
      }
    }
  }

}
