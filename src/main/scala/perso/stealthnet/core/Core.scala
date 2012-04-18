package perso.stealthnet.core

import org.jboss.netty.channel.Channel
import perso.stealthnet.network.StealthNetConnectionsManager
import perso.stealthnet.core.cryptography.RijndaelParameters
import perso.stealthnet.network.protocol.{
  Command,
  RSAParametersClientCommand,
  RSAParametersServerCommand,
  RijndaelParametersClientCommand,
  RijndaelParametersServerCommand
}
import perso.stealthnet.util.{EmptyLoggingContext, Logging}

object Core extends Logging with EmptyLoggingContext {

  var stopping = false

  def processCommand(command: Command, channel: Channel) {
    /* XXX - handle all commands */
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

      case _ =>
        logger error("Unhandled command")
    }
  }

}
