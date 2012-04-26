package stealthnet.scala.sandbox

import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import stealthnet.scala.core.Core
import stealthnet.scala.network.StealthNetServer
import stealthnet.scala.network.StealthNetConnectionsManager
import stealthnet.scala.webservices.{UpdateClient, WebCacheClient}
import stealthnet.scala.network.WebCaches

object TestServer {

  def main(args: Array[String]): Unit = {
    println("Started")

    Security.addProvider(new BouncyCastleProvider())
    try {
      Core.start()

      Thread.sleep(60000)
    }
    finally {
      Core.stop()
    }
  }

}
