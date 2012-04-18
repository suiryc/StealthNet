package perso.stealthnet.util

object Peer {

  /* IPv6 ? */
  val regexp = "^([^:]+):(\\d+)$".r

}

case class Peer(host: String, port: Int) {

  override def toString(): String = host + ":" + port

}
