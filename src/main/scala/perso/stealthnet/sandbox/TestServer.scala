package perso.stealthnet.sandbox

import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import perso.stealthnet.network.StealthNetServer
import perso.stealthnet.network.StealthNetConnectionsManager
import perso.stealthnet.webservices.{UpdateClient, WebCacheClient}
import perso.stealthnet.network.WebCaches

object TestServer {

  def main(args: Array[String]): Unit = {
    println("Started")

    Security.addProvider(new BouncyCastleProvider())
    StealthNetConnectionsManager.start()

    val online = true
    if (online) {
      /* XXX - move to Server or Core ? */
      WebCaches.refresh()
      WebCaches.addPeer()
    }

    try {
      StealthNetServer.start()

      Thread.sleep(40000)
    }
    finally {
      StealthNetServer.stop()

      /* XXX - move to Server or Core ? */
      if (online)
        WebCaches.removePeer()

      StealthNetConnectionsManager.stop()
    }
  }

}
