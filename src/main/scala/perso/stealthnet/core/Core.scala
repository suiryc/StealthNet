package perso.stealthnet.core

import org.jboss.netty.channel.Channel
import perso.stealthnet.network.StealthNetConnections
import perso.stealthnet.core.cryptography.RijndaelParameters
import perso.stealthnet.core.util.{EmptyLoggingContext, Logging}
import perso.stealthnet.network.protocol.{
  Command,
  RSAParametersClientCommand,
  RSAParametersServerCommand,
  RijndaelParametersClientCommand,
  RijndaelParametersServerCommand
}

object Core extends Logging with EmptyLoggingContext {

  def processCommand(command: Command, channel: Channel) {
    /* XXX - handle all commands */
    command match {
      case c: RSAParametersServerCommand =>
        /* XXX - check this is not our public key */
        StealthNetConnections.get(channel).remoteRSAKey = c.key
        channel.write(new RSAParametersClientCommand())

      case c: RSAParametersClientCommand =>
        /* XXX - check this is not our public key */
        val cnx = StealthNetConnections.get(channel)
        cnx.remoteRSAKey = c.key
        cnx.localRijndaelParameters = RijndaelParameters()
        channel.write(new RijndaelParametersServerCommand(cnx.localRijndaelParameters))

      case c: RijndaelParametersServerCommand =>
        val cnx = StealthNetConnections.get(channel)
        cnx.remoteRijndaelParameters = c.parameters
        cnx.localRijndaelParameters = RijndaelParameters()
        channel.write(new RijndaelParametersClientCommand(cnx.localRijndaelParameters))
        cnx.established = true

      case c: RijndaelParametersClientCommand =>
        val cnx = StealthNetConnections.get(channel)
        cnx.remoteRijndaelParameters = c.parameters
        cnx.established = true

      case _ =>
        logger error("Unhandled command")
    }
  }

}
