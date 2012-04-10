package perso.stealthnet.network.protocol

import java.io.InputStream
import perso.stealthnet.core.cryptography.{Hash, RSAKeys}

object RSAParametersCommand extends CommandBuilder {

  val code = 0x10 byteValue

  def read(input: InputStream): Command = {
    val modulus = ProtocolStream.readBytes(input)
    val exponent = ProtocolStream.readBytes(input)

    new RSAParametersCommand(modulus, exponent)
  }

}

/* Note: public key modulus/exponent as big-endian byte array */
class RSAParametersCommand(val modulus: Array[Byte], val exponent: Array[Byte])
  extends Command(code = RSAParametersCommand.code, encryption = Encryption.None)
{

  assert(modulus != null)
  assert(exponent != null)

  def this() = this(RSAKeys.publicKey.getModulus.toByteArray(),
      RSAKeys.publicKey.getPublicExponent.toByteArray())

  def arguments(): List[Any] = List(modulus, exponent)

}
