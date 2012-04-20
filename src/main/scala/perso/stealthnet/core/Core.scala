package perso.stealthnet.core

import perso.stealthnet.network.{StealthNetConnection, StealthNetConnectionsManager}
import perso.stealthnet.cryptography.{RijndaelParameters, RSAKeys}
import perso.stealthnet.network.protocol.commands._
import perso.stealthnet.util.{EmptyLoggingContext, Logging}

/**
 * Core object, processing commands.
 */
object Core extends Logging with EmptyLoggingContext {

  /** Whether we are stopping the application. */
  var stopping = false

  /**
   * Process incoming command.
   *
   * @param command command to process
   * @param cnx related connection
   */
  def processCommand(command: Command, cnx: StealthNetConnection) {
    command match {
      case c: RSAParametersServerCommand =>
        if ((c.key.getModulus == RSAKeys.publicKey.getModulus)
            && (c.key.getPublicExponent == RSAKeys.publicKey.getPublicExponent))
        {
          /* connecting to ourselves ? */
          cnx.closing = true
          cnx.channel.close
          return
        }
        cnx.remoteRSAKey = c.key
        cnx.channel.write(new RSAParametersClientCommand())

      case c: RSAParametersClientCommand =>
        if ((c.key.getModulus == RSAKeys.publicKey.getModulus)
            && (c.key.getPublicExponent == RSAKeys.publicKey.getPublicExponent))
        {
          /* connecting to ourselves ? */
          cnx.closing = true
          cnx.channel.close
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

      case c: Command21 =>
        /* XXX - handle */

      case c: Command22 =>
        /* XXX - handle */

      case c: Command23 =>
        /* XXX - handle */

      case c: Command50 =>
        /* XXX - handle */

      case c: Command51 =>
        /* XXX - handle */

      case c: Command52 =>
        /* XXX - handle */

      case c: Command53 =>
        /* XXX - handle */

      case c: Command54 =>
        /* XXX - handle */

      case c: Command60 =>
        /* XXX - handle */

      case c: Command61 =>
        /* XXX - handle */

      case c: Command62 =>
        /* XXX - handle */

      case c: Command63 =>
        /* XXX - handle */

      case c: Command64 =>
        /* XXX - handle */

      case c: Command70 =>
        /* XXX - handle */

      case c: Command71 =>
        /* XXX - handle */

      case c: Command72 =>
        /* XXX - handle */

      case c: Command74 =>
        /* XXX - handle */

      case c: Command75 =>
        /* XXX - handle */

      case c: Command76 =>
        /* XXX - handle */

      case c: Command78 =>
        /* XXX - handle */

      case c: Command79 =>
        /* XXX - handle */

      case c: Command7A =>
        /* XXX - handle */

      case _ =>
        logger error(cnx.loggerContext, "Unhandled command " + command)
    }
  }

}
