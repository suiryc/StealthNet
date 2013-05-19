package stealthnet.scala.core

import java.security.Security
import java.util.{Timer, TimerTask}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import stealthnet.scala.Settings
import stealthnet.scala.cryptography.{RijndaelParameters, RSAKeys}
import stealthnet.scala.network.{
  StealthNetClient,
  StealthNetServer,
  WebCachesManager
}
import stealthnet.scala.network.connection.{
  StealthNetConnection,
  StealthNetConnectionsManager
}
import stealthnet.scala.network.protocol.commands._
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}

/**
 * Core object, processing commands.
 */
object Core extends Logging with EmptyLoggingContext {

  /* Register BouncyCastle if necessary */
  if (Option(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)) == None)
    Security.addProvider(new BouncyCastleProvider())

  /** Core actor system. */
  val actorSystem = stealthnet.scala.actor.System

  /** Shared timer. */
  val timer = new Timer()

  /** Whether we are stopping the application. */
  var stopping = false

  /**
   * Process received command.
   *
   * @todo handle all commands
   * @param command command to process
   * @param cnx related connection
   */
  // scalastyle:off method.length
  def receivedCommand(command: Command, cnx: StealthNetConnection) {
    cnx.receivedCommands += 1
    command match {
      case c: RSAParametersServerCommand =>
        if ((c.key.getModulus == RSAKeys.publicKey.getModulus)
            && (c.key.getPublicExponent == RSAKeys.publicKey.getPublicExponent))
        {
          /* connecting to ourself ? */
          cnx.close()
          return
        }
        cnx.remoteRSAKey = Some(c.key)
        cnx.channel.write(new RSAParametersClientCommand())

      case c: RSAParametersClientCommand =>
        if ((c.key.getModulus == RSAKeys.publicKey.getModulus)
            && (c.key.getPublicExponent == RSAKeys.publicKey.getPublicExponent))
        {
          /* connecting to ourself ? */
          cnx.close()
          return
        }
        cnx.remoteRSAKey = Some(c.key)
        val rijndaelParameters = RijndaelParameters()
        cnx.localRijndaelParameters = rijndaelParameters
        cnx.channel.write(new RijndaelParametersServerCommand(rijndaelParameters))

      case c: RijndaelParametersServerCommand =>
        cnx.remoteRijndaelParameters = c.parameters
        val rijndaelParameters = RijndaelParameters()
        cnx.localRijndaelParameters = rijndaelParameters
        cnx.channel.write(new RijndaelParametersClientCommand(rijndaelParameters))
        cnx.established = true

      case c: RijndaelParametersClientCommand =>
        cnx.remoteRijndaelParameters = c.parameters
        cnx.established = true

      case c: SearchCommand =>

      case c: Command21 =>

      case c: Command22 =>

      case c: Command23 =>

      case c: Command50 =>

      case c: Command51 =>

      case c: Command52 =>

      case c: Command53 =>

      case c: Command54 =>

      case c: Command60 =>

      case c: Command61 =>

      case c: Command62 =>

      case c: Command63 =>

      case c: Command64 =>

      case c: Command70 =>

      case c: Command71 =>

      case c: Command72 =>

      case c: Command74 =>

      case c: Command75 =>

      case c: Command76 =>

      case c: Command78 =>

      case c: Command79 =>

      case c: Command7A =>

      case _ =>
        logger error(cnx.loggerContext, s"Unhandled command $command")
    }
  }
  // scalastyle:on method.length

  /**
   * Starts core.
   *
   * Shall be called only once.
   *
   * Performs the following actions:
   *   - refreshes the WebCaches list
   *   - starts the ''StealthNet'' server
   *   - starts the connections manager
   *
   * @see [[stealthnet.scala.network.WebCachesManager]]
   * @see [[stealthnet.scala.network.StealthNetServer]]
   * @see [[stealthnet.scala.network.connection.StealthNetConnectionsManager]]
   */
  def start() {
    stopping = false
    WebCachesManager.refresh()
    if (Settings.core.enableServerConnections)
      StealthNetServer.start()
    StealthNetConnectionsManager.start()
  }

  /**
   * Stops core.
   *
   * Performs the following actions:
   *   - removes ourself from WebCaches if necessary
   *     - this is also done by the connections manager upon stopping, but it is
   *       better to do it as soon as possible
   *   - stops the connections manager
   * The connections manager is expected to call back the `shutdown` method once
   * all connections are terminated.
   *
   * @see [[stealthnet.scala.network.WebCachesManager]]
   * @see [[stealthnet.scala.network.connection.StealthNetConnectionsManager]]
   * @see [[stealthnet.scala.core.Core]].`shutdown`
   */
  def stop() {
    stopping = true
    WebCachesManager.removePeer()
    StealthNetConnectionsManager.stop()
  }

  /**
   * Shutdowns core.
   *
   * Performs the following actions:
   *   - stops the ''StealthNet'' server and cleans client shared resources
   *   - terminates the shared timer
   *
   * @see [[stealthnet.scala.network.StealthNetServer]]
   * @see [[stealthnet.scala.network.StealthNetClient]]
   */
  def shutdown() {
    StealthNetServer.stop()
    StealthNetClient.stop()
    timer.cancel()
  }

  /**
   * Schedules a delayed task.
   *
   * @param action task to perform
   * @param delay delay (ms) before performing task
   */
  def schedule(action: => Unit, delay: Long) {
    timer.schedule(new TimerTask() {
      def run() = action
    }, delay)
  }

}
