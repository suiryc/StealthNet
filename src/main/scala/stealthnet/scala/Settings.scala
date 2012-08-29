package stealthnet.scala

import com.typesafe.config.{Config, ConfigFactory}
import scala.collection.JavaConversions._

/** ''StealthNet'' core settings companion object. */
object Settings {

  /** Core settings. */
  val core = new Settings(ConfigFactory.load())

}

/** Base class for settings. */
abstract class BaseSettings(config: Config) {

  protected val confPath: String

  protected def optionPath(name: String) = confPath + '.' + name

  /* XXX - triggers Exception ? */
  //config.checkValid(ConfigFactory.defaultReference(), confPath)

}

/** ''StealthNet'' core settings. */
class Settings(config: Config) extends BaseSettings(config) {

  protected val confPath = "stealthnet.core"

  /** Whether to debug I/O data. `false` by default. */
  val debugIOData: Boolean =
    config.getBoolean(optionPath("debug.io.data"))

  /** Whether to debug I/O commands. `false` by default. */
  val debugIOCommands: Boolean =
    config.getBoolean(optionPath("debug.io.commands"))

  /** Connection timeout (ms). `5s` by default. */
  val connectTimeout: Long =
    config.getMilliseconds(optionPath("timeout.connection"))

  /** Connection read timeout (ms). `30s` by default. */
  val readTimeout: Long =
    config.getMilliseconds(optionPath("timeout.read"))

  /** Connection write timeout (ms). `30s` by default. */
  val writeTimeout: Long =
    config.getMilliseconds(optionPath("timeout.write"))

  /** ''StealthNet'' server port. `6097` by default. */
  val serverPort: Int =
    config.getInt(optionPath("server.port"))

  /** Whether to enable server connections. For testing purposes. */
  val enableServerConnections: Boolean =
    config.getBoolean(optionPath("connection.server"))

  /** Whether to enable client connections. For testing purposes. */
  val enableClientConnections: Boolean =
    config.getBoolean(optionPath("connection.client"))

  /**
   * Average connection count. `6` by default.
   * `1 + average/4` more connections are actually allowed: new connections are
   * sought when being below the average count and until the maximum is reached.
   */
  val avgCnxCount: Int =
    config.getInt(optionPath("connection.average"))

  /**
   * Whether to get WebCaches from ''RShare'' update server, or use the default
   * ones. `true` by default.
   */
  val wsWebCacheUpdateEnabled: Boolean =
    config.getBoolean(optionPath("webservice.webcache.update.enable"))

  /**
   * ''RShare'' update URL. `http://rshare.de/rshareupdates.asmx` by
   * default.
   */
  val wsWebCacheUpdateURL: String =
    config.getString(optionPath("webservice.webcache.update.url"))

  /**
   * ''RShare'' default WebCaches. `http://rshare.de/rshare.asmx` and
   * `http://webcache.stealthnet.at/rwpmws.php` by default.
   */
  val wsWebCacheDefault: List[String] =
    config.getStringList(optionPath("webservice.webcache.default")).toList

  /** Regular expressions of WebCaches to exclude. None by default. */
  val wsWebCacheExcluded: List[String] =
    config.getStringList(optionPath("webservice.webcache.exclude")).toList

  /**
   * WebCaches check period (ms). `30s` by default.
   * Used when peer adding failed on WebCaches.
   */
  val wsWebCacheCheckPeriod: Long =
    config.getMilliseconds(optionPath("webservice.webcache.check.period"))

}
