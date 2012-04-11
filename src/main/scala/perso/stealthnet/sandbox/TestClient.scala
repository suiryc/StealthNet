package perso.stealthnet.sandbox

import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import perso.stealthnet.network.StealthNetClient
import perso.stealthnet.webservices.{UpdateClient, WebCacheClient}

object TestClient {

  def main(args: Array[String]): Unit = {
    println("Started")

    /* XXX - JCE Unlimited Strength Jurisdiction Policy Files are needed for keys > 128-bits */
    Security.addProvider(new BouncyCastleProvider())

    val webCaches = UpdateClient.getWebCaches("http://rshare.de/rshareupdates.asmx") match {
      case Some(webCaches) => webCaches
      case None => Nil
    }

    val peerRe = "^([^:]+):(\\d+)$".r
    var host: String = null
    var port: Int = 0
    for (webCache <- webCaches if (port == 0)) {
      println("WebCache: " + webCache)
      //WebCacheClient.addPeer(webCache, 6097)
      WebCacheClient.getPeer(webCache) match {
        case Some(peer) => println("Got peer: " + peer)
          peer match {
            case peerRe(h, p) =>
              host = h
              port = Integer.parseInt(p)
            case _ =>
          }
        case None =>
      }
      //WebCacheClient.removePeer(webCache)
    }

    var client1: StealthNetClient = null
    try {
      //client1 = new StealthNetClient("127.0.0.1", 8080)
      client1 = new StealthNetClient(host, port)
      client1.start()

      Thread.sleep(5000)
    }
    finally {
      if (client1 != null) {
        client1.stop()
      }
    }
  }

}
