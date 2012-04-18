package perso.stealthnet.network.protocol

import java.io.InputStream
import perso.stealthnet.core.cryptography.RijndaelParameters
import perso.stealthnet.core.cryptography.CipherMode
import perso.stealthnet.core.cryptography.PaddingMode

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

  val code = 0x12 byteValue

  def read(input: InputStream): Command =
    new RijndaelParametersServerCommand(RijndaelParametersCommand.readRijndaelParameters(input))

}

object RijndaelParametersClientCommand extends CommandBuilder {

  val code = 0x13 byteValue

  def read(input: InputStream): Command =
    new RijndaelParametersClientCommand(RijndaelParametersCommand.readRijndaelParameters(input))

}

/* Note: public key modulus/exponent as big-endian byte array */
abstract class RijndaelParametersCommand extends Command {

  val encryption = Encryption.RSA

  val parameters: RijndaelParameters

  assert(parameters != null)

  def arguments(): List[(String, Any)] = List(
    "blockSize" -> parameters.blockSize,
    "feedbackSize" -> parameters.feedbackSize,
    "keySize" -> parameters.keySize,
    "cipherMode" -> CipherMode.id(parameters.cipherMode),
    "paddingMode" -> PaddingMode.id(parameters.paddingMode),
    "iv" -> new ByteArrayArgument(parameters.iv, BitSize.Byte),
    "key" -> new ByteArrayArgument(parameters.key, BitSize.Byte)
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
