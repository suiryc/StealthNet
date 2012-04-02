package perso.stealthnet.webservices

/**
 * WebCache client.
 */
object WebCacheClient {

  /**
   * GetPeer SOAP action.
   *
   * @param url URL to call
   * @return peer (ip:port) upon success
   */
  def getPeer(url: String): Option[String] = {
    SoapClient.doRequest(url, <GetPeer xmlns="http://rshare.de/rshare.asmx" />) match {
      case Left(l) =>
        /* XXX - report failure in logs ? */
        println("Failed: " + l)
        None

      case Right(r) =>
        (r \\ "GetPeerResult").text match {
          case "" =>
            /* XXX - report failure in logs ? */
            println("No peer received")
            None

          case peer =>
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
        /* XXX - report failure in logs ? */
        println("Failed: " + l)
        false

      case Right(r) =>
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
        /* XXX - report failure in logs ? */
        println("Failed: " + l)
        false

      case Right(r) =>
        true
    }
  }

}
