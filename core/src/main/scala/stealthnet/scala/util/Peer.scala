package stealthnet.scala.util

import java.net.{Inet4Address, InetAddress, InetSocketAddress}
import scala.util.matching.Regex


/** Peer companion object. */
object Peer {

  /** Regular expression to extract host and port number. */
  val regexp: Regex = "^(.+):(\\d+)$".r

  def apply(host: String, port: Int) =
    new Peer(new InetSocketAddress(InetAddress.getByName(host), port))

}

/**
 * Peer.
 *
 * @note Being a case class, the `hashCode` and `equals` methods are
 *   automatically generated using the class parameters.
 */
case class Peer(addr: InetSocketAddress) {

  val host: String = addr.getAddress.getHostAddress

  val port: Int = addr.getPort

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
