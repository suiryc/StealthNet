package stealthnet.scala.ui.web

import com.typesafe.config.{Config, ConfigFactory}
import stealthnet.scala.{BaseSettings, Settings => coreSettings}

/** ''StealthNet'' web UI settings companion object. */
object Settings {

  /** Core settings. */
  val core = coreSettings.core

  /** Web UI settings. */
  val ui = new Settings(ConfigFactory.load())

}

/** ''StealthNet'' web UI settings. */
class Settings(config: Config) extends BaseSettings(config) {

  protected val confPath = "stealthnet.ui.web"

  /** Web server port. */
  val webServerPort = config.getInt(optionPath("server.port"))

  /** Shutdown grace period (ms). `2s` by default. */
  val shutdownGracePeriod: Long =
    config.getMilliseconds(optionPath("server.shutdown.grace.period"))

}
