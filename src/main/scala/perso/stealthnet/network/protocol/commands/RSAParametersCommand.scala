package perso.stealthnet.network.protocol.commands

import java.io.InputStream
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import perso.stealthnet.core.cryptography.RSAKeys
import perso.stealthnet.core.cryptography.Ciphers._
import perso.stealthnet.network.protocol.{Encryption, ProtocolStream}

protected object RSAParametersCommand {

  def readKeySpec(input: InputStream): RSAPublicKeySpec = {
    val modulus = ProtocolStream.readBigInteger(input)
    val exponent = ProtocolStream.readBigInteger(input)

    new RSAPublicKeySpec(modulus.bigInteger, exponent.bigInteger)
  }

}

object RSAParametersServerCommand extends CommandBuilder {

  val code: Byte = 0x10

  def read(input: InputStream): Command =
    new RSAParametersServerCommand(RSAParametersCommand.readKeySpec(input))

}

object RSAParametersClientCommand extends CommandBuilder {

  val code: Byte = 0x11

  def read(input: InputStream): Command =
    new RSAParametersClientCommand(RSAParametersCommand.readKeySpec(input))

}

abstract class RSAParametersCommand extends Command {

  val encryption = Encryption.None

  val key: RSAPublicKey

  assert(key != null)

  def arguments() = List(
    "modulus" -> BigIntegerArgument(key.getModulus),
    "exponent" -> BigIntegerArgument(key.getPublicExponent)
  )

}

class RSAParametersServerCommand(val key: RSAPublicKey)
  extends RSAParametersCommand
{

  val code = RSAParametersServerCommand.code

  def this() = this(RSAKeys.publicKey)

}

class RSAParametersClientCommand(val key: RSAPublicKey)
  extends RSAParametersCommand
{

  val code = RSAParametersClientCommand.code

  def this() = this(RSAKeys.publicKey)

}
