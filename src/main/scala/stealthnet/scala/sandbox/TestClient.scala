package stealthnet.scala.sandbox

import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import stealthnet.scala.network.StealthNetClient
import stealthnet.scala.network.StealthNetConnectionsManager
import stealthnet.scala.webservices.{UpdateClient, WebCacheClient}
import stealthnet.scala.network.protocol.commands.SearchCommand
import stealthnet.scala.network.WebCaches
import stealthnet.scala.util.Peer
import stealthnet.scala.Config
import stealthnet.scala.core.Core

object TestClient {

  def main(args: Array[String]): Unit = {
    println("Started")

    Security.addProvider(new BouncyCastleProvider())
    StealthNetConnectionsManager.start()

    var client: StealthNetClient = null
    try {
      client = new StealthNetClient(Peer("127.0.0.1", Config.serverPort))
      client.start()

      Thread.sleep(10000)
      client.write(SearchCommand("intouchables"))

      Thread.sleep(10000)
    }
    finally {
      if (client != null)
        client.stop()
      Core.stop()
    }
  }

}
