package stealthnet.scala

/**
 * Configuration object.
 */
object Config {

  /** Whether to debug I/O data. `false` by default. */
  val debugIO = false

  /** Connection timeout (ms). `5s` by default. */
  val connectTimeout = 5000

  /** Connection read timeout (ms). `30s` by default. */
  val readTimeout = 30000

  /** Connection write timeout (ms). `30s` by default. */
  val writeTimeout = 30000

  /** ''StealthNet'' server port. */
  val serverPort = 6097

  /** Whether to enable server connections. For testing purposes. */
  val enableServerConnections = true

  /** Whether to enable client connections. For testing purposes. */
  val enableClientConnections = true

  /**
   * Average connection count. `1 + average/4` more connections are actually
   * allowed: new connections are sought when being below the average count
   * and until the maximum is reached.
   */
  val avgCnxCount = 1

}
