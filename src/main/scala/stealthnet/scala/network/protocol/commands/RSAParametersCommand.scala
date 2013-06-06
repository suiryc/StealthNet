package stealthnet.scala.network.protocol.commands

import java.io.InputStream
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import stealthnet.scala.cryptography.RSAKeys
import stealthnet.scala.cryptography.Ciphers._
import stealthnet.scala.network.protocol.{Encryption, ProtocolStream}

/**
 * ''RSA'' parameters command helper object.
 */
protected object RSAParametersCommand {

  /** Gets the arguments definition. */
  def argumentDefinitions = List(
    BigIntegerArgumentDefinition("modulus"),
    BigIntegerArgumentDefinition("exponent")
  )

  /**
   * Reads the ''RSA'' public key specifications.
   *
   * @param arguments map of read arguments
   * @return a new [[java.security.interfaces.RSAPublicKey]] representing the
   *   read parameters
   */
  def readKeySpec(arguments: Map[String, Any]): RSAPublicKeySpec = {
    val modulus = arguments("modulus").asInstanceOf[BigInt]
    val exponent = arguments("exponent").asInstanceOf[BigInt]

    new RSAPublicKeySpec(modulus.bigInteger, exponent.bigInteger)
  }

}

/**
 * ''RSA'' parameters command base class.
 *
 * The protocol do use two commands to exchange ''RSA'' parameters:
 *   - [[stealthnet.scala.network.protocol.commands.RSAParametersServerCommand]]
 *     sent by the server
 *   - [[stealthnet.scala.network.protocol.commands.RSAParametersClientCommand]]
 *     sent by the client
 *
 * This class gathers the properties shared by both.
 */
protected abstract class RSAParametersCommand extends Command {

  val encryption = Encryption.None

  /** The actual ''RSA'' public key. */
  val key: RSAPublicKey

  // scalastyle:off null
  assert(key != null)
  // scalastyle:on null

  def argumentDefinitions = RSAParametersCommand.argumentDefinitions

  def arguments = Map(
    "modulus" -> new BigInt(key.getModulus),
    "exponent" -> new BigInt(key.getPublicExponent)
  )

}

/** ''RSA'' server parameters command companion object. */
object RSAParametersServerCommand extends CommandBuilder {

  val code: Byte = 0x10

  def argumentDefinitions = RSAParametersCommand.argumentDefinitions

  def read(input: InputStream): Command =
    new RSAParametersServerCommand(RSAParametersCommand.readKeySpec(readArguments(input)))

}

/**
 * ''RSA'' server parameters command.
 *
 * Command code: `0x10`
 */
class RSAParametersServerCommand(val key: RSAPublicKey)
  extends RSAParametersCommand
{

  val code = RSAParametersServerCommand.code

  /** Ctor with our ''RSA'' public key. */
  def this() = this(RSAKeys.publicKey)

}

/** ''RSA'' client parameters command companion object. */
object RSAParametersClientCommand extends CommandBuilder {

  val code: Byte = 0x11

  def argumentDefinitions = RSAParametersCommand.argumentDefinitions

  def read(input: InputStream): Command =
    new RSAParametersClientCommand(RSAParametersCommand.readKeySpec(readArguments(input)))

}

/**
 * ''RSA'' client parameters command.
 *
 * Command code: `0x11`
 */
class RSAParametersClientCommand(val key: RSAPublicKey)
  extends RSAParametersCommand
{

  val code = RSAParametersClientCommand.code

  /** Ctor with our ''RSA'' public key. */
  def this() = this(RSAKeys.publicKey)

}
