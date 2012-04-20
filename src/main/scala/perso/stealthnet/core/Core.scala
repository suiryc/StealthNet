package perso.stealthnet.core

import org.jboss.netty.channel.Channel
import perso.stealthnet.network.StealthNetConnectionsManager
import perso.stealthnet.cryptography.RijndaelParameters
import perso.stealthnet.network.protocol.commands._
import perso.stealthnet.util.{EmptyLoggingContext, Logging}

object Core extends Logging with EmptyLoggingContext {

  var stopping = false

  def processCommand(command: Command, channel: Channel) {
    command match {
      case c: RSAParametersServerCommand =>
        /* XXX - check this is not our public key */
        StealthNetConnectionsManager.getConnection(channel).remoteRSAKey = c.key
        channel.write(new RSAParametersClientCommand())

      case c: RSAParametersClientCommand =>
        /* XXX - check this is not our public key */
        val cnx = StealthNetConnectionsManager.getConnection(channel)
        cnx.remoteRSAKey = c.key
        cnx.localRijndaelParameters = RijndaelParameters()
        channel.write(new RijndaelParametersServerCommand(cnx.localRijndaelParameters))

      case c: RijndaelParametersServerCommand =>
        val cnx = StealthNetConnectionsManager.getConnection(channel)
        cnx.remoteRijndaelParameters = c.parameters
        cnx.localRijndaelParameters = RijndaelParameters()
        channel.write(new RijndaelParametersClientCommand(cnx.localRijndaelParameters))
        cnx.established = true

      case c: RijndaelParametersClientCommand =>
        val cnx = StealthNetConnectionsManager.getConnection(channel)
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
        logger error("Unhandled command " + command)
    }
  }

}
