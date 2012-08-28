package stealthnet.scala.sandbox

import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import stealthnet.scala.core.Core
import stealthnet.scala.network.StealthNetServer
import stealthnet.scala.network.connection.StealthNetConnectionsManager
import stealthnet.scala.webservices.{UpdateClient, WebCacheClient}
import stealthnet.scala.network.WebCaches

// scalastyle:off magic.number regex
object TestServer {

  def main(args: Array[String]): Unit = {
    println("Started")

    try {
      Core.start()

      Thread.sleep(60000)
    }
    finally {
      Core.stop()
    }

    println("Finished")
  }

}
// scalastyle:on magic.number regex
