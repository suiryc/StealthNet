package stealthnet.scala.sandbox

import stealthnet.scala.core.Core
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}

// scalastyle:off magic.number regex
object TestServer extends Logging with EmptyLoggingContext {

  def main(args: Array[String]): Unit = {
    logger.info("Started")

    try {
      Core.start()

      Thread.sleep(60000)
    }
    finally {
      Core.stop()
    }

    logger.info("Finished")
  }

}
// scalastyle:on magic.number regex
