package stealthnet.scala.util

/**
 * Peer companion object.
 *
 * @todo Handle IPv6 ?
 */
object Peer {

  /** Regular expression to extract IP and port number. */
  val regexp = "^([^:]+):(\\d+)$".r

}

/**
 * Peer.
 *
 * @note Being a case class, the `hashCode` and `equals` methods are
 *   automatically generated using the class parameters.
 */
case class Peer(host: String, port: Int) {

  override def toString(): String = host + ":" + port

}
