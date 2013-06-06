package stealthnet.scala.sandbox

import stealthnet.scala.core.Core

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
