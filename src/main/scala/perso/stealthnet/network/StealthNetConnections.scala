package perso.stealthnet.network

import java.net.InetSocketAddress
import com.weiglewilczek.slf4s.Logging
import scala.collection.mutable.HashSet
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelLocal
import org.jboss.netty.channel.group.ChannelGroup

class StealthNetConnectionParameters(
    var group: ChannelGroup = null,
    var isClient: Boolean = false
) {

  var accepted: Boolean = true
  var closing: Boolean = false
  var host: String = null
  var port: Int = _

}

class StealthNetConnection protected[network] (val channel: Channel)
    extends StealthNetConnectionParameters()
{
  assert(channel != null)
}

object StealthNetConnections extends Logging {

  private val hosts = new HashSet[String]
  private val local: ChannelLocal[StealthNetConnection] = new ChannelLocal(false)

  def add(host: String): Boolean = {
    /* XXX - configuration */
    var avgCnxCount = 10

    if (avgCnxCount > 10)
      avgCnxCount = 10

    synchronized {
      if (hosts.size >= 1.25 * avgCnxCount) {
        logger debug("Refused connection with host[" + host + "]: limit reached")
        false
      }
      else if (hosts.contains(host)) {
        logger debug("Refused connection with host[" + host + "]: already connected")
        false
      }
      else {
        logger debug("Accepted connection with host[" + host + "]")
        hosts.add(host)
        true
      }
    }
  }

  private def remove(host: String) = {
    if (host != null) {
      synchronized {
        hosts.remove(host)
      }
    }
  }

  def accept(cnx: StealthNetConnection): Boolean = {
    val remoteAddress = cnx.channel.getRemoteAddress

    cnx.accepted = remoteAddress match {
      case socketAddress: InetSocketAddress =>
        val address = socketAddress.getAddress.getHostAddress
        cnx.host = address
        cnx.port = socketAddress.getPort
        /* Client-side connection host was checked and added beforehand */
        if (cnx.isClient || add(address))
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
        remove(cnx.host)
      return false
    }

    true
  }

  def get(channel: Channel, create: Boolean = true): StealthNetConnection = {
    var cnx = local.get(channel)

    if ((cnx == null) && create) {
      /* initialize the connection object */
      cnx = new StealthNetConnection(channel)
      val tmp = local.setIfAbsent(channel, cnx)
      if (tmp != null) {
        /* woops, someone else striked first */
        cnx = tmp
      }
    }

    cnx
  }

  def closed(channel: Channel) = {
    val cnx = local.remove(channel)

    if ((cnx != null) && cnx.accepted)
      remove(cnx.host)

    cnx
  }

}
