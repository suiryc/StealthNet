package stealthnet.scala.network.protocol.commands

import java.io.{
  ByteArrayInputStream,
  ByteArrayOutputStream,
  InputStream,
  OutputStream
}
import javax.crypto.{CipherInputStream, CipherOutputStream}
import scala.collection.mutable.WrappedArray
import stealthnet.scala.{Constants, Settings}
import stealthnet.scala.cryptography.{
  Algorithm,
  Hash,
  Message,
  RSAKeys
}
import stealthnet.scala.cryptography.Ciphers._
import stealthnet.scala.cryptography.io.{
  BCCipherInputStream,
  BCCipherOutputStream
}
import stealthnet.scala.network.connection.StealthNetConnection
import stealthnet.scala.network.protocol.{BitSize, Encryption, ProtocolStream}
import stealthnet.scala.network.protocol.exceptions.InvalidDataException
import stealthnet.scala.util.{HexDumper, UUID}
import stealthnet.scala.util.io.DebugInputStream
import stealthnet.scala.util.log.{EmptyLoggingContext, Logging}

/**
 * Command builder.
 */
trait CommandBuilder
  extends CommandArgumentsReader[Command]
  with CommandArgumentDefinitions
{

  /** Command code handled by this builder. */
  val code: Byte

}

/**
 * Command class companion object.
 */
object Command extends Logging with EmptyLoggingContext {

  /** Known command builders. */
  private val builders: Map[Byte, CommandBuilder] =
    Map() ++ List[CommandBuilder](RSAParametersServerCommand, RSAParametersClientCommand,
        RijndaelParametersServerCommand, RijndaelParametersClientCommand,
        SearchCommand, Command21, Command22, Command23, Command50, Command51,
        Command52, Command53, Command54, Command60, Command61, Command62,
        Command63, Command64, Command70, Command71, Command72, Command74,
        Command75, Command76, Command78, Command79, Command7A).map(c => (c.code, c))

  /**
   * Generates a new random id.
   *
   * ''SHA-384'' hash of a random ''UUID''.
   *
   * @see [[stealthnet.scala.util.UUID]].`generate`
   */
  def generateId(): Hash =
    Message.hash(UUID.generate().bytes, Algorithm.SHA384)

  /**
   * Gets the command builder handling a given command code.
   *
   * @param code command code to handle
   * @return an option value containing the builder, or `None` if none
   */
  def commandBuilder(code: Byte) = builders.get(code)

  /**
   * Builder helper.
   *
   * State machine which can rebuild a command by reading an input stream.
   *
   * This state machine has many steps and provides two methods to advance:
   * `readHeader` and `readCommand`.
   *
   * Each time `readHeader` is called, it reads the data related to the current
   * state if it is one of (in order):
   *   - protocol header
   *   - encryption mode
   *   - command length
   *
   * Once the command length is reached, and until the command is fully read
   * with `readCommand`, `readHeader` returns the command length. In any other
   * state, `-1` is returned, indicating it needs to be called again to
   * advance one step until reaching the next command length.
   */
  class Builder {

    /** Available states. */
    private object State extends Enumeration {
      /** State: reading protocol header. */
      val Header = Value
      /** State: reading encryption mode. */
      val Encryption = Value
      /** State: reading command length. */
      val Length = Value
      /** State: reading command. */
      val Content = Value
    }

    /** Current state. */
    private var state = State.Header
    /** Current encryption mode. */
    private var encryption: Encryption.Value = _
    /** Current command length. */
    private var length: Int = _

    /**
     * Decrypts data.
     *
     * Used for debugging purposes.
     *
     * @param cnx concerned connection
     * @param cipher encrypted data
     * @param offset data offset
     * @param length data length
     * @return decrypted data
     */
    def decryptData(cnx: StealthNetConnection, cipher: Array[Byte],
      offset: Int, lenght: Int): Array[Byte] =
    {
      if (encryption == Encryption.None)
        return cipher

      val input: InputStream = new ByteArrayInputStream(cipher, offset, length)
      val cipherInput: InputStream = encryption match {
        case Encryption.RSA =>
          new CipherInputStream(input, rsaDecrypter(RSAKeys.privateKey))

        case Encryption.Rijndael =>
          cnx.rijndaelDecrypter map { rijndaelDecrypter =>
            rijndaelDecrypter.reset()
            new BCCipherInputStream(input, rijndaelDecrypter)
          } getOrElse {
            throw new Exception("Cannot decrypt: Rijndael decrypter not yet created")
          }
      }

      val output = new ByteArrayOutputStream()
      val buffer = new Array[Byte](Constants.cipherBufferLength)
      var read = 1
      while (read > 0) {
        read = cipherInput.read(buffer)
        if (read > 0)
          output.write(buffer, 0, read)
      }
      output.close()
      output.toByteArray()
    }

    /**
     * Gets decrypting data.
     *
     * Used for debugging purposes.
     *
     * @param cnx concerned connection
     * @return decrypting data
     */
    def decryptingData(cnx: StealthNetConnection): String = encryption match {
      case Encryption.None =>
        "Data is not encrypted"

      case Encryption.RSA =>
        RSAKeys.privateKey.toString()

      case Encryption.Rijndael =>
        cnx.remoteRijndaelParameters.toString()
    }

    /**
     * Reads and decrypts command.
     *
     * @param cnx concerned connection
     * @param input stream to read from
     * @return an option value containing the rebuilt command, or `None` if
     *   decrypted data does not correspond to a known command
     * @todo When receiving unknown commands, instead of skipping, increment
     *   counter and drop connection if too many ?
     */
    def readCommand(cnx: StealthNetConnection, input: InputStream): Option[Command] = {
      assert(state == State.Content)

      /* cipher-text section */
      val cipherInput: InputStream = encryption match {
        case Encryption.None =>
          input

        case Encryption.RSA =>
          new CipherInputStream(input, rsaDecrypter(RSAKeys.privateKey))

        case Encryption.Rijndael =>
          cnx.rijndaelDecrypter map { rijndaelDecrypter =>
            rijndaelDecrypter.reset()
            new BCCipherInputStream(input, rijndaelDecrypter)
          } getOrElse {
            throw new Exception("Cannot decrypt: Rijndael decrypter not yet created")
          }
      }
      val newInput = if (Settings.core.debugIOData && (cipherInput ne input))
          new DebugInputStream(cipherInput, cnx.loggerContext ++ List("step" -> "decrypted"))
        else
          cipherInput

      val code = ProtocolStream.readByte(newInput)
      val command = commandBuilder(code) match {
        case Some(builder) =>
          Some(builder.read(newInput))

        case None =>
          logger error(cnx.loggerContext, "Unknown command code[0x%02X] with encryption[%s]".format(code, encryption))
          None
      }

      state = State.Header
      command
    }

    /**
     * Reads protocol packet header.
     *
     * @param cnx concerned connection
     * @param input stream to read from
     * @return command length if read, or `-1`
     */
    def readHeader(cnx: StealthNetConnection, input: InputStream): Int = {
      state match {
      case State.Header =>
        val headerRAW = ProtocolStream.read(input, Constants.protocolRAW.length)
        /* Note: bare Arrays cannot be compared with '==', unlike wrapped ones */
        if ((headerRAW:WrappedArray[Byte]) != (Constants.protocolRAW:WrappedArray[Byte]))
          throw new InvalidDataException("Invalid protocol header:\n" + HexDumper.dump(headerRAW), loggerContext = cnx.loggerContext)
        state = State.Encryption
        -1

      case State.Encryption =>
        val encryptionRAW = ProtocolStream.readByte(input)
        encryption = Encryption.value(encryptionRAW) match {
          case Some(mode) =>
            mode

          case None =>
            throw new InvalidDataException("Invalid encryption[%02X]".format(encryptionRAW), loggerContext = cnx.loggerContext)
        }
        state = State.Length
        -1

      case State.Length =>
        length = ProtocolStream.readInteger(input, BitSize.Short).intValue
        state = State.Content
        length

      case _ =>
        length
      }
    }

  }

}

/**
 * Protocol command.
 */
abstract class Command extends CommandArguments {

  /** Command code. */
  val code: Byte

  /** Encryption mode. */
  val encryption: Encryption.Value

  /**
   * Writes this command.
   *
   * Writes the protocol header, encryption mode, command length and command
   * arguments.
   *
   * @param cnx concerned connection
   * @param output stream to write to
   * @return number of (unencrypted) written bytes for command arguments
   */
  def write(cnx: StealthNetConnection, output: OutputStream): Int = {
    /* plain-text section */
    ProtocolStream.writeHeader(output)
    ProtocolStream.writeByte(output, Encryption.id(encryption))
    ProtocolStream.writeInteger(output, 0, BitSize.Short)
    output.flush()

    /* cipher-text section */
    val cipherOutput: OutputStream = encryption match {
      case Encryption.None =>
        output

      case Encryption.RSA =>
        cnx.remoteRSAKey map { key =>
          new CipherOutputStream(output, rsaEncrypter(key))
        } getOrElse {
          throw new Exception("Cannot encrypt: remote RSA key not yet known")
        }

      case Encryption.Rijndael =>
        cnx.rijndaelEncrypter map { rijndaelEncrypter =>
          rijndaelEncrypter.reset()
          new BCCipherOutputStream(output, rijndaelEncrypter)
        } getOrElse {
          throw new Exception("Cannot encrypt: Rijndael encrypter not yet created")
        }
    }

    var unencryptedLength: Int = 0
    unencryptedLength += ProtocolStream.writeByte(cipherOutput, code)
    unencryptedLength += write(cipherOutput)
    cipherOutput.flush()
    cipherOutput.close()

    unencryptedLength
  }

  override def toString = getClass.getSimpleName +
    "(code=" + code.formatted("0x%02X") + ", arguments=" + argumentsToString + ")"

}
