package stealthnet.scala.sandbox

import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import stealthnet.scala.network.StealthNetServer
import stealthnet.scala.network.StealthNetConnectionsManager
import stealthnet.scala.webservices.{UpdateClient, WebCacheClient}
import stealthnet.scala.network.WebCaches

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
