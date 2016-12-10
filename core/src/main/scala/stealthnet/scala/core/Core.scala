package stealthnet.scala.core

import java.security.Security
import java.util.{Timer, TimerTask}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import stealthnet.scala.Settings
import stealthnet.scala.cryptography.{RijndaelParameters, RSAKeys}
import stealthnet.scala.network.{StealthNetClient, StealthNetServer}
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
  if (Option(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)).isEmpty)
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
        cnx.send(new RSAParametersClientCommand())
        ()

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
        cnx.send(new RijndaelParametersServerCommand(rijndaelParameters))
        ()

      case c: RijndaelParametersServerCommand =>
        cnx.remoteRijndaelParameters = c.parameters
        val rijndaelParameters = RijndaelParameters()
        cnx.localRijndaelParameters = rijndaelParameters
        cnx.send(new RijndaelParametersClientCommand(rijndaelParameters))
        cnx.established = true

      case c: RijndaelParametersClientCommand =>
        cnx.remoteRijndaelParameters = c.parameters
        cnx.established = true

      case _: SearchCommand =>

      case _: Command21 =>

      case _: Command22 =>

      case _: Command23 =>

      case _: Command50 =>

      case _: Command51 =>

      case _: Command52 =>

      case _: Command53 =>

      case _: Command54 =>

      case _: Command60 =>

      case _: Command61 =>

      case _: Command62 =>

      case _: Command63 =>

      case _: Command64 =>

      case _: Command70 =>

      case _: Command71 =>

      case _: Command72 =>

      case _: Command74 =>

      case _: Command75 =>

      case _: Command76 =>

      case _: Command78 =>

      case _: Command79 =>

      case _: Command7A =>

      case _ =>
        logger.error(cnx.loggerContext, s"Unhandled command $command")
    }
  }
  // scalastyle:on method.length

  /**
   * Starts core.
   *
   * Shall be called only once.
   *
   * Performs the following actions:
   *   - starts the ''StealthNet'' server
   *   - starts the connections manager
   *
   * @see [[stealthnet.scala.network.StealthNetServer]]
   * @see [[stealthnet.scala.network.connection.StealthNetConnectionsManager]]
   */
  def start() {
    stopping = false
    if (Settings.core.enableServerConnections)
      StealthNetServer.start()
    StealthNetConnectionsManager.start()
  }

  /**
   * Stops core.
   *
   * Performs the following actions:
   *   - stops the connections manager
   * The connections manager is expected to call back the `shutdown` method once
   * all connections are terminated.
   *
   * @see [[stealthnet.scala.network.connection.StealthNetConnectionsManager]]
   * @see [[stealthnet.scala.core.Core]].`shutdown`
   */
  def stop() {
    stopping = true
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
    val f1 = StealthNetServer.stop()
    val f2 = StealthNetClient.stop()

    import scala.concurrent.ExecutionContext.Implicits.global
    for {
      _ <- f1
      _ <- f2
    } logger.debug("Shutdowned")

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
      def run(): Unit = action
    }, delay)
  }

}
