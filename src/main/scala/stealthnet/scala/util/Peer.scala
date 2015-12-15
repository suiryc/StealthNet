package stealthnet.scala.util

import java.net.{InetAddress, Inet4Address, InetSocketAddress}


/** Peer companion object. */
object Peer {

  /** Regular expression to extract host and port number. */
  val regexp = "^(.+):(\\d+)$".r

  def apply(host: String, port: Int) = {
    val actualHost =
      if (!host.contains(':') || host.contains("::")) host
      else {
        if (host.endsWith(":")) s"$host:"
        else s"$host::"
      }
    new Peer(new InetSocketAddress(InetAddress.getByName(actualHost), port))
  }

}

/**
 * Peer.
 *
 * @note Being a case class, the `hashCode` and `equals` methods are
 *   automatically generated using the class parameters.
 */
case class Peer(addr: InetSocketAddress) {

  val host = addr.getAddress.getHostAddress

  val port = addr.getPort

  private lazy val representation =
    addr.getAddress match {
      case _: Inet4Address =>
        s"$host:$port"

      case _ =>
        s"[$host]:$port"
    }

  override def toString: String =
    representation

}
