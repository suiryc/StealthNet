package perso.stealthnet.network.protocol

import java.io.InputStream
import perso.stealthnet.core.cryptography.{Hash, RSAKeys}

object RSAParametersServerCommand extends CommandBuilder {

  val code = 0x10 byteValue

  def read(input: InputStream): Command = {
    val modulus = ProtocolStream.readBytes(input)
    val exponent = ProtocolStream.readBytes(input)

    new RSAParametersServerCommand(modulus, exponent)
  }

}

object RSAParametersClientCommand extends CommandBuilder {

  val code = 0x11 byteValue

  def read(input: InputStream): Command = {
    val modulus = ProtocolStream.readBytes(input)
    val exponent = ProtocolStream.readBytes(input)

    new RSAParametersClientCommand(modulus, exponent)
  }

}

/* Note: public key modulus/exponent as big-endian byte array */
class RSAParametersCommand(
    override val code: Byte,
    val modulus: Array[Byte],
    val exponent: Array[Byte]
  )
  extends Command(code, Encryption.None)
{

  assert(modulus != null)
  assert(exponent != null)

  def arguments(): List[Any] = List(modulus, exponent)

}

class RSAParametersServerCommand(
    override val modulus: Array[Byte],
    override val exponent: Array[Byte]
  )
  extends RSAParametersCommand(RSAParametersServerCommand.code, modulus, exponent)
{

  def this() = this(RSAKeys.publicKey.getModulus.toByteArray(),
      RSAKeys.publicKey.getPublicExponent.toByteArray())

}

class RSAParametersClientCommand(
    override val modulus: Array[Byte],
    override val exponent: Array[Byte]
  )
  extends RSAParametersCommand(RSAParametersClientCommand.code, modulus, exponent)
{

  def this() = this(RSAKeys.publicKey.getModulus.toByteArray(),
      RSAKeys.publicKey.getPublicExponent.toByteArray())

}
