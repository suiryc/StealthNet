package stealthnet.scala.network.protocol.commands

import java.io.InputStream
import stealthnet.scala.cryptography.{
  RijndaelParameters,
  CipherMode,
  PaddingMode
}
import stealthnet.scala.network.protocol.{BitSize, Encryption}
import stealthnet.scala.network.protocol.exceptions.InvalidDataException

/**
 * ''Rijndael'' parameters command helper object.
 */
protected object RijndaelParametersCommand {

  /** Gets the arguments definition. */
  def argumentDefinitions = List(
    IntegerArgumentDefinition("blockSize", BitSize.Short),
    IntegerArgumentDefinition("feedbackSize", BitSize.Short),
    IntegerArgumentDefinition("keySize", BitSize.Short),
    ByteArgumentDefinition("cipherMode"),
    ByteArgumentDefinition("paddingMode"),
    ByteArrayArgumentDefinition("iv", BitSize.Byte),
    ByteArrayArgumentDefinition("key", BitSize.Byte)
  )

  /**
   * Reads the ''Rijndael'' parameters.
   *
   * @param arguments map of read arguments
   * @return a new [[stealthnet.scala.cryptography.RijndaelParameters]]
   *   representing the read parameters
   */
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

/**
 * ''Rijndael'' parameters command base class.
 *
 * The protocol do use two commands to exchange ''Rijndael'' parameters:
 *   - [[stealthnet.scala.network.protocol.commands.RijndaelParametersServerCommand]]
 *     sent by the server
 *   - [[stealthnet.scala.network.protocol.commands.RijndaelParametersClientCommand]]
 *     sent by the client
 *
 * This class gathers the properties shared by both.
 */
protected abstract class RijndaelParametersCommand extends Command {

  val encryption = Encryption.RSA

  /** The actual ''Rijndael'' parameters. */
  val parameters: RijndaelParameters

  // scalastyle:off null
  assert(parameters != null)
  // scalastyle:on null

  def argumentDefinitions: List[CommandArgumentDefinition] = RijndaelParametersCommand.argumentDefinitions

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

/** ''Rijndael'' server parameters command companion object. */
object RijndaelParametersServerCommand extends CommandBuilder {

  val code: Byte = 0x12

  def argumentDefinitions: List[CommandArgumentDefinition] = RijndaelParametersCommand.argumentDefinitions

  def read(input: InputStream): Command =
    new RijndaelParametersServerCommand(RijndaelParametersCommand.readRijndaelParameters(readArguments(input)))

}

/**
 * ''Rijndael'' server parameters command.
 *
 * Command code: `0x12`
 */
class RijndaelParametersServerCommand(val parameters: RijndaelParameters)
  extends RijndaelParametersCommand
{

  val code: Byte = RijndaelParametersServerCommand.code

}

/** ''Rijndael'' client parameters command companion object. */
object RijndaelParametersClientCommand extends CommandBuilder {

  val code: Byte = 0x13

  def argumentDefinitions: List[CommandArgumentDefinition] = RijndaelParametersCommand.argumentDefinitions

  def read(input: InputStream): Command =
    new RijndaelParametersClientCommand(RijndaelParametersCommand.readRijndaelParameters(readArguments(input)))

}

/**
 * ''Rijndael'' client parameters command.
 *
 * Command code: `0x13`
 */
class RijndaelParametersClientCommand(val parameters: RijndaelParameters)
  extends RijndaelParametersCommand
{

  val code: Byte = RijndaelParametersClientCommand.code

}
