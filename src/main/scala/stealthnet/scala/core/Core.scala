package stealthnet.scala.core

import java.util.{Timer, TimerTask}
import stealthnet.scala.Config
import stealthnet.scala.cryptography.{RijndaelParameters, RSAKeys}
import stealthnet.scala.network.{
  StealthNetConnection,
  StealthNetConnectionsManager,
  StealthNetServer,
  WebCaches
}
import stealthnet.scala.network.protocol.commands._
import stealthnet.scala.ui.web.JettyServer
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}

/**
 * Core object, processing commands.
 */
object Core extends Logging with EmptyLoggingContext {

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
        cnx.remoteRSAKey = c.key
        cnx.channel.write(new RSAParametersClientCommand())

      case c: RSAParametersClientCommand =>
        if ((c.key.getModulus == RSAKeys.publicKey.getModulus)
            && (c.key.getPublicExponent == RSAKeys.publicKey.getPublicExponent))
        {
          /* connecting to ourself ? */
          cnx.close()
          return
        }
        cnx.remoteRSAKey = c.key
        cnx.localRijndaelParameters = RijndaelParameters()
        cnx.channel.write(new RijndaelParametersServerCommand(cnx.localRijndaelParameters))

      case c: RijndaelParametersServerCommand =>
        cnx.remoteRijndaelParameters = c.parameters
        cnx.localRijndaelParameters = RijndaelParameters()
        cnx.channel.write(new RijndaelParametersClientCommand(cnx.localRijndaelParameters))
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
        logger error(cnx.loggerContext, "Unhandled command " + command)
    }
  }

  /**
   * Starts core.
   *
   * Shall be called only once.
   *
   * Performs the following actions:
   *   - refreshes the WebCaches list
   *   - starts the ''StealthNet'' server
   *   - starts the connections manager
   *   - starts the web server
   *
   * @see [[stealthnet.scala.network.WebCaches]]
   * @see [[stealthnet.scala.network.StealthNetServer]]
   * @see [[stealthnet.scala.network.StealthNetConnectionsManager]]
   * @see [[stealthnet.scala.ui.web.JettyServer]]
   */
  def start() {
    stopping = false
    WebCaches.refresh()
    if (Config.enableServerConnections)
      StealthNetServer.start()
    StealthNetConnectionsManager.start()
    JettyServer.start()
  }

  /**
   * Stops core.
   *
   * Performs the following actions:
   *   - removes ourself from WebCaches if necessary
   *     - this is also done by the connections manager upon stopping, but it is
   *       better to do it as soon as possible
   *   - stops the ''StealthNet'' server
   *   - stops the connections manager
   *   - stops the web server
   *   - terminates the shared timer
   *
   * @see [[stealthnet.scala.network.WebCaches]]
   * @see [[stealthnet.scala.network.StealthNetServer]]
   * @see [[stealthnet.scala.network.StealthNetConnectionsManager]]
   * @see [[stealthnet.scala.ui.web.JettyServer]]
   */
  def stop() {
    stopping = true
    WebCaches.removePeer()
    StealthNetServer.stop()
    StealthNetConnectionsManager.stop()
    JettyServer.stop()
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
