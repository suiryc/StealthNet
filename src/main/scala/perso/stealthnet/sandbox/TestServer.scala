package perso.stealthnet.sandbox

import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import perso.stealthnet.network.StealthNetServer
import perso.stealthnet.webservices.{UpdateClient, WebCacheClient}

object TestServer {

  def main(args: Array[String]): Unit = {
    println("Started")

    /* XXX - JCE Unlimited Strength Jurisdiction Policy Files are needed for keys > 128-bits */
    Security.addProvider(new BouncyCastleProvider())

    val webCaches = UpdateClient.getWebCaches("http://rshare.de/rshareupdates.asmx") match {
      case Some(webCaches) => webCaches
      case None => Nil
    }

    for (webCache <- webCaches) {
      println("WebCache: " + webCache)
      WebCacheClient.addPeer(webCache, 6097)
      WebCacheClient.getPeer(webCache) match {
        case Some(peer) => println("Got peer: " + peer)
        case None =>
      }
    }

    try {
      StealthNetServer.start()

      Thread.sleep(20000)
    }
    finally {
      StealthNetServer.stop()

      for (webCache <- webCaches) {
        println("WebCache: " + webCache)
        WebCacheClient.removePeer(webCache)
      }
    }
  }

}
