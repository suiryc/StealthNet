package stealthnet.scala.network.protocol.commands

import java.io.InputStream
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import stealthnet.scala.cryptography.RSAKeys
import stealthnet.scala.cryptography.Ciphers._
import stealthnet.scala.network.protocol.{Encryption, ProtocolStream}

protected object RSAParametersCommand {

  def argumentDefinitions = List(
    BigIntegerArgumentDefinition("modulus"),
    BigIntegerArgumentDefinition("exponent")
  )

  def readKeySpec(arguments: Map[String, Any]): RSAPublicKeySpec = {
    val modulus = arguments("modulus").asInstanceOf[BigInt]
    val exponent = arguments("exponent").asInstanceOf[BigInt]

    new RSAPublicKeySpec(modulus.bigInteger, exponent.bigInteger)
  }

}

protected abstract class RSAParametersCommand extends Command {

  val encryption = Encryption.None

  val key: RSAPublicKey

  assert(key != null)

  def argumentDefinitions = RSAParametersCommand.argumentDefinitions

  def arguments = Map(
    "modulus" -> new BigInt(key.getModulus),
    "exponent" -> new BigInt(key.getPublicExponent)
  )

}

object RSAParametersServerCommand extends CommandBuilder {

  val code: Byte = 0x10

  def argumentDefinitions = RSAParametersCommand.argumentDefinitions

  def read(input: InputStream): Command =
    new RSAParametersServerCommand(RSAParametersCommand.readKeySpec(readArguments(input)))

}

class RSAParametersServerCommand(val key: RSAPublicKey)
  extends RSAParametersCommand
{

  val code = RSAParametersServerCommand.code

  def this() = this(RSAKeys.publicKey)

}

object RSAParametersClientCommand extends CommandBuilder {

  val code: Byte = 0x11

  def argumentDefinitions = RSAParametersCommand.argumentDefinitions

  def read(input: InputStream): Command =
    new RSAParametersClientCommand(RSAParametersCommand.readKeySpec(readArguments(input)))

}

class RSAParametersClientCommand(val key: RSAPublicKey)
  extends RSAParametersCommand
{

  val code = RSAParametersClientCommand.code

  def this() = this(RSAKeys.publicKey)

}
