package stealthnet.scala.network.protocol.commands

import java.io.InputStream
import stealthnet.scala.cryptography.{
  RijndaelParameters,
  CipherMode,
  PaddingMode
}
import stealthnet.scala.network.protocol.{BitSize, Encryption, ProtocolStream}
import stealthnet.scala.network.protocol.exceptions.InvalidDataException

protected object RijndaelParametersCommand {

  def argumentDefinitions = List(
    IntegerArgumentDefinition("blockSize", BitSize.Short),
    IntegerArgumentDefinition("feedbackSize", BitSize.Short),
    IntegerArgumentDefinition("keySize", BitSize.Short),
    ByteArgumentDefinition("cipherMode"),
    ByteArgumentDefinition("paddingMode"),
    ByteArrayArgumentDefinition("iv", BitSize.Byte),
    ByteArrayArgumentDefinition("key", BitSize.Byte)
  )

  def readRijndaelParameters(arguments: Map[String, Any]): RijndaelParameters = {
    val blockSize = arguments("blockSize").asInstanceOf[Long].intValue
    val feedbackSize = arguments("feedbackSize").asInstanceOf[Long].intValue
    val keySize = arguments("keySize").asInstanceOf[Long].intValue
    val cipherModeId = arguments("cipherMode").asInstanceOf[Byte]
    val cipherMode = CipherMode.value(cipherModeId) match {
      case Some(mode) =>
        mode

      case None =>
        throw new InvalidDataException("Invalid cipher mode[%02X]".format(cipherModeId))
    }
    val paddingModeId = arguments("paddingMode").asInstanceOf[Byte]
    val paddingMode = PaddingMode.value(paddingModeId) match {
      case Some(mode) =>
        mode

      case None =>
        throw new InvalidDataException("Invalid padding mode[%02X]".format(paddingModeId))
    }
    val iv = arguments("iv").asInstanceOf[Array[Byte]]
    val key = arguments("key").asInstanceOf[Array[Byte]]

    new RijndaelParameters(blockSize, feedbackSize, keySize, cipherMode,
      paddingMode, iv, key)
  }

}

protected abstract class RijndaelParametersCommand extends Command {

  val encryption = Encryption.RSA

  val parameters: RijndaelParameters

  assert(parameters != null)

  def argumentDefinitions = RijndaelParametersCommand.argumentDefinitions

  def arguments = Map(
    "blockSize" -> parameters.blockSize,
    "feedbackSize" -> parameters.feedbackSize,
    "keySize" -> parameters.keySize,
    "cipherMode" -> CipherMode.id(parameters.cipherMode),
    "paddingMode" -> PaddingMode.id(parameters.paddingMode),
    "iv" -> parameters.iv,
    "key" -> parameters.key
  )

}

object RijndaelParametersServerCommand extends CommandBuilder {

  val code: Byte = 0x12

  def argumentDefinitions = RijndaelParametersCommand.argumentDefinitions

  def read(input: InputStream): Command =
    new RijndaelParametersServerCommand(RijndaelParametersCommand.readRijndaelParameters(readArguments(input)))

}

class RijndaelParametersServerCommand(val parameters: RijndaelParameters)
  extends RijndaelParametersCommand
{

  val code = RijndaelParametersServerCommand.code

}

object RijndaelParametersClientCommand extends CommandBuilder {

  val code: Byte = 0x13

  def argumentDefinitions = RijndaelParametersCommand.argumentDefinitions

  def read(input: InputStream): Command =
    new RijndaelParametersClientCommand(RijndaelParametersCommand.readRijndaelParameters(readArguments(input)))

}

class RijndaelParametersClientCommand(val parameters: RijndaelParameters)
  extends RijndaelParametersCommand
{

  val code = RijndaelParametersClientCommand.code

}
