package perso.stealthnet.webservices

import com.weiglewilczek.slf4s.Logging

/**
 * WebCache client.
 */
object WebCacheClient extends Logging {

  /**
   * GetPeer SOAP action.
   *
   * @param url URL to call
   * @return peer (ip:port) upon success
   */
  def getPeer(url: String): Option[String] = {
    SoapClient.doRequest(url, <GetPeer xmlns="http://rshare.de/rshare.asmx" />) match {
      case Left(l) =>
        logger error ("Failed to get peer from service[" + url + "]: " + l)
        None

      case Right(r) =>
        (r \\ "GetPeerResult").text match {
          case "" =>
            logger debug ("Got no peer from service[" + url + "]")
            None

          case peer =>
            logger debug ("Got peer[" + peer + "] from service[" + url + "]")
            Some(peer)
        }
    }
  }

  /**
   * AddPeer SOAP action.
   *
   * @param url URL to call
   * @param port local port accepting incoming connections
   * @return true upon success
   */
  def addPeer(url: String, port: Int): Boolean = {
    SoapClient.doRequest(url, <AddPeer xmlns="http://rshare.de/rshare.asmx"><port>{port}</port></AddPeer>) match {
      case Left(l) =>
        logger error ("Failed to add (self) peer on service[" + url + "]: " + l)
        println("Failed: " + l)
        false

      case Right(r) =>
        logger debug ("Added (self) peer on service[" + url + "]")
        true
    }
  }

  /**
   * RemovePeer SOAP action.
   *
   * @param url URL to call
   * @return true upon success
   */
  def removePeer(url: String): Boolean = {
    SoapClient.doRequest(url, <RemovePeer xmlns="http://rshare.de/rshare.asmx" />) match {
      case Left(l) =>
        logger error ("Failed to remove (self) peer from service[" + url + "]: " + l)
        false

      case Right(r) =>
        logger debug ("Removed (self) peer from service[" + url + "]")
        true
    }
  }

}
