package perso.stealthnet.network

import java.net.InetSocketAddress
import java.security.interfaces.RSAPublicKey
import scala.actors.Actor
import scala.collection.mutable.HashSet
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelLocal
import org.jboss.netty.channel.group.ChannelGroup
import perso.stealthnet.core.cryptography.RijndaelParameters
import perso.stealthnet.util.{EmptyLoggingContext, Logging, LoggingContext, Peer}

class StealthNetConnectionParameters(
    var group: ChannelGroup = null,
    var isClient: Boolean = false
) extends LoggingContext
{

  def loggerContext = {
    if (peer != null)
      List("peer" -> peer)
    else
      List.empty
  }

  var peer: Peer = null
  var accepted: Boolean = true
  var established: Boolean = false
  var closing: Boolean = false
  var remoteRSAKey: RSAPublicKey = null
  var localRijndaelParameters: RijndaelParameters = null
  var remoteRijndaelParameters: RijndaelParameters = null

}

class StealthNetConnection protected[network] (val channel: Channel)
    extends StealthNetConnectionParameters()
{
  assert(channel != null)
}

object StealthNetConnectionsManager extends Actor with Logging with EmptyLoggingContext {

  private val hosts = new HashSet[String]
  private val local: ChannelLocal[StealthNetConnection] = new ChannelLocal(false)

  case class AddPeer(peer: Peer)
  case class AddConnection(cnx: StealthNetConnection)
  case class GetConnection(channel: Channel, create: Boolean = true)
  case class ClosedChannel(channel: Channel)
  case class Stop()

  /* XXX - configuration */
  //protected val avgCnxCount = 10
  protected var avgCnxCount = 1

  if (avgCnxCount > 10)
    avgCnxCount = 10

  def act() {
    loop {
      react {
        case AddPeer(peer) =>
          reply(add(peer))

        case AddConnection(cnx) =>
          reply(add(cnx))

        case GetConnection(channel, create) =>
          reply(get(channel, create))

        case ClosedChannel(channel) =>
          reply(closed(channel))

        case Stop() =>
          /* time to leave */
          logger debug("Stopping")
          exit()
      }
    }
  }

  private def add(peer: Peer): Boolean = {
    if (hosts.size >= 1.25 * avgCnxCount) {
      /* XXX - ok here ? */
      WebCaches.removePeer()
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
      true
    }
  }

  private def add(cnx: StealthNetConnection): Boolean = {
    val remoteAddress = cnx.channel.getRemoteAddress

    cnx.accepted = remoteAddress match {
      case socketAddress: InetSocketAddress =>
        val address = socketAddress.getAddress.getHostAddress
        cnx.peer = Peer(address, socketAddress.getPort)
        /* Client-side connection host was checked and added beforehand */
        if (cnx.isClient || add(cnx.peer))
          true
        else
          false

      case _ =>
        /* shall not happen */
        logger debug("Refused connection with endpoint[" + remoteAddress + "]: unhandled address type")
        false
    }

    if (!cnx.accepted) {
      local.remove(cnx.channel)
      if (cnx.isClient)
        remove(cnx.peer)
      return false
    }

    true
  }

  private def get(channel: Channel, create: Boolean): StealthNetConnection = {
    var cnx = local.get(channel)

    if ((cnx == null) && create) {
      /* initialize the connection object */
      cnx = new StealthNetConnection(channel)
      local.set(channel, cnx)
    }

    cnx
  }

  private def closed(channel: Channel) = {
    val cnx = local.remove(channel)

    if ((cnx != null) && cnx.accepted)
      remove(cnx.peer)

    cnx
  }

  private def remove(peer: Peer) {
    if (peer == null)
      return

    hosts.remove(peer.host)

    /* XXX - ok here ? */
    if (hosts.size < avgCnxCount)
      WebCaches.addPeer()
  }

  def getConnection(channel: Channel, create: Boolean = true): StealthNetConnection =
    (this !? GetConnection(channel, create)).asInstanceOf[StealthNetConnection]

  def addConnection(cnx: StealthNetConnection): Boolean =
    (this !? AddConnection(cnx)).asInstanceOf[Boolean]

  def addPeer(peer: Peer): Boolean =
    (this !? AddPeer(peer)).asInstanceOf[Boolean]

  def stop() = this ! Stop()

}
