package perso.stealthnet.network.protocol

import java.io.InputStream
import java.math.BigInteger
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import perso.stealthnet.core.cryptography.{Hash, RSAKeys}
import perso.stealthnet.core.cryptography.Ciphers._

protected object RSAParametersCommand {

  def readKeySpec(input: InputStream): RSAPublicKeySpec = {
    val modulus = ProtocolStream.readBigInteger(input)
    val exponent = ProtocolStream.readBigInteger(input)

    new RSAPublicKeySpec(modulus, exponent)
  }

}

object RSAParametersServerCommand extends CommandBuilder {

  val code = 0x10 byteValue

  def read(input: InputStream): Command =
    new RSAParametersServerCommand(RSAParametersCommand.readKeySpec(input))

}

object RSAParametersClientCommand extends CommandBuilder {

  val code = 0x11 byteValue

  def read(input: InputStream): Command =
    new RSAParametersClientCommand(RSAParametersCommand.readKeySpec(input))

}

abstract class RSAParametersCommand extends Command {

  val encryption = Encryption.None

  val key: RSAPublicKey

  assert(key != null)

  def arguments(): List[(String, Any)] = List(
    "modulus" -> key.getModulus,
    "exponent" -> key.getPublicExponent
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
