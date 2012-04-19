package perso.stealthnet.sandbox

import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import perso.stealthnet.network.StealthNetClient
import perso.stealthnet.network.StealthNetConnectionsManager
import perso.stealthnet.webservices.{UpdateClient, WebCacheClient}
import perso.stealthnet.network.protocol.commands.SearchCommand
import perso.stealthnet.network.WebCaches
import perso.stealthnet.util.Peer

object TestClient {

  def main(args: Array[String]): Unit = {
    println("Started")

    Security.addProvider(new BouncyCastleProvider())
    StealthNetConnectionsManager.start()

    val online = false
    val peerRe = "^([^:]+):(\\d+)$".r
    val peer = if (online) {
        WebCaches.refresh()
        WebCaches.addPeer()
        val result = WebCaches.getPeer()
        WebCaches.removePeer()
        result
      }
      else
        Peer("127.0.0.1", 6097)

    var client1: StealthNetClient = null
    try {
      client1 = new StealthNetClient(peer)
      client1.start()

      Thread.sleep(10000)
      client1.write(SearchCommand("intouchables"))

      Thread.sleep(120000)
    }
    finally {
      if (client1 != null)
        client1.stop()
      StealthNetConnectionsManager.stop()
    }
  }

}
