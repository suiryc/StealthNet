package stealthnet.scala.webservices

import stealthnet.scala.util.Peer
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}

/**
 * WebCache client.
 */
object WebCacheClient extends Logging with EmptyLoggingContext {

  /**
   * ''GetPeer'' ''SOAP'' action.
   *
   * @param url ''URL'' to call
   * @return an option value containing the retrieved peer, or `None` if none
   */
  def getPeer(url: String): Option[Peer] = {
    SoapClient.doRequest(url, <GetPeer xmlns="http://rshare.de/rshare.asmx" />) match {
      case Left(l) =>
        logger.error(s"Failed to get peer from service[$url]: $l")
        None

      case Right(r) =>
        (r \\ "GetPeerResult").text match {
          case "" =>
            logger.debug(s"Got no peer from service[$url]")
            None

          case Peer.regexp(host, port) =>
            logger.debug(s"Got host[$host] port[$port] from service[$url]")
            // Note: WebCache accepts but does not handle correctly IPv6
            // addresses. We may receive one from it, but the value is truncated
            // to the max size of an IPv4 one (15 chars = 4*3 digits + 3 dots)
            try {
              Some(Peer(host, Integer.parseInt(port)))
            }
            catch {
              case e: Throwable =>
                logger.error(s"Invalid peer address[$host]", e)
                None
            }

          case peer =>
            logger.debug(s"Got unhandled peer[$peer] from service[$url]")
            None
        }
    }
  }

  /**
   * ''AddPeer'' ''SOAP'' action.
   *
   * @param url ''URL'' to call
   * @param port local port accepting incoming connections
   * @return true upon success
   */
  def addPeer(url: String, port: Int): Boolean = {
    SoapClient.doRequest(url, <AddPeer xmlns="http://rshare.de/rshare.asmx"><port>{port}</port></AddPeer>) match {
      case Left(l) =>
        logger.error(s"Failed to add (self) peer on service[$url]: $l")
        false

      case Right(r) =>
        logger.debug(s"Added (self) peer port[$port] on service[$url]")
        true
    }
  }

  /**
   * ''RemovePeer'' ''SOAP'' action.
   *
   * @param url ''URL'' to call
   * @return true upon success
   */
  def removePeer(url: String): Boolean = {
    SoapClient.doRequest(url, <RemovePeer xmlns="http://rshare.de/rshare.asmx" />) match {
      case Left(l) =>
        logger.error(s"Failed to remove (self) peer from service[$url]: $l")
        false

      case Right(r) =>
        logger.debug(s"Removed (self) peer from service[$url]")
        true
    }
  }

}
