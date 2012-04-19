package perso.stealthnet.network.protocol.commands

import java.io.InputStream
import perso.stealthnet.core.cryptography.{
  RijndaelParameters,
  CipherMode,
  PaddingMode
}
import perso.stealthnet.network.protocol.{BitSize, Encryption, ProtocolStream}

protected object RijndaelParametersCommand {

  def readRijndaelParameters(input: InputStream): RijndaelParameters = {
    val blockSize = ProtocolStream.readInteger(input, BitSize.Short).intValue
    val feedbackSize = ProtocolStream.readInteger(input, BitSize.Short).intValue
    val keySize = ProtocolStream.readInteger(input, BitSize.Short).intValue
    val cipherMode = CipherMode.value(ProtocolStream.readByte(input))
    val paddingMode = PaddingMode.value(ProtocolStream.readByte(input))
    val iv = ProtocolStream.readBytes(input, BitSize.Byte)
    val key = ProtocolStream.readBytes(input, BitSize.Byte)

    new RijndaelParameters(blockSize, feedbackSize, keySize, cipherMode,
      paddingMode, iv, key)
  }

}

object RijndaelParametersServerCommand extends CommandBuilder {

  val code: Byte = 0x12

  def read(input: InputStream): Command =
    new RijndaelParametersServerCommand(RijndaelParametersCommand.readRijndaelParameters(input))

}

object RijndaelParametersClientCommand extends CommandBuilder {

  val code: Byte = 0x13

  def read(input: InputStream): Command =
    new RijndaelParametersClientCommand(RijndaelParametersCommand.readRijndaelParameters(input))

}

/* Note: public key modulus/exponent as big-endian byte array */
abstract class RijndaelParametersCommand extends Command {

  val encryption = Encryption.RSA

  val parameters: RijndaelParameters

  assert(parameters != null)

  def arguments() = List(
    "blockSize" -> IntegerArgument(parameters.blockSize, BitSize.Short),
    "feedbackSize" -> IntegerArgument(parameters.feedbackSize, BitSize.Short),
    "keySize" -> IntegerArgument(parameters.keySize, BitSize.Short),
    "cipherMode" -> ByteArgument(CipherMode.id(parameters.cipherMode)),
    "paddingMode" -> ByteArgument(PaddingMode.id(parameters.paddingMode)),
    "iv" -> ByteArrayArgument(parameters.iv, BitSize.Byte),
    "key" -> ByteArrayArgument(parameters.key, BitSize.Byte)
  )

}

class RijndaelParametersServerCommand(val parameters: RijndaelParameters)
  extends RijndaelParametersCommand
{

  val code = RijndaelParametersServerCommand.code

}

class RijndaelParametersClientCommand(val parameters: RijndaelParameters)
  extends RijndaelParametersCommand
{

  val code = RijndaelParametersClientCommand.code

}
