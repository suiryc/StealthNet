package perso.stealthnet.network.protocol.commands

import java.io.{
  ByteArrayInputStream,
  ByteArrayOutputStream,
  InputStream,
  OutputStream
}
import javax.crypto.{CipherInputStream, CipherOutputStream}
import scala.collection.mutable.WrappedArray
import perso.stealthnet.core.cryptography.{
  Algorithm,
  Hash,
  Message,
  RSAKeys
}
import perso.stealthnet.core.cryptography.Ciphers._
import perso.stealthnet.core.cryptography.io.{BCCipherInputStream, BCCipherOutputStream}
import perso.stealthnet.network.StealthNetConnection
import perso.stealthnet.network.protocol.{BitSize, Constants, Encryption, ProtocolStream}
import perso.stealthnet.network.protocol.exceptions.InvalidDataException
import perso.stealthnet.util.{EmptyLoggingContext, HexDumper, Logging, UUID}

trait CommandBuilder extends CommandArgumentBuilder[Command] {

  val code: Byte

}

object Command extends Logging with EmptyLoggingContext {

  private val builders: Map[Byte, CommandBuilder] =
    Map() ++ List[CommandBuilder](RSAParametersServerCommand, RSAParametersClientCommand,
        RijndaelParametersServerCommand, RijndaelParametersClientCommand,
        SearchCommand, Command21, Command22).map(c => (c.code, c))

  def generateId(): Hash = Message.hash(UUID.generate().bytes, Algorithm.SHA384)

  class Builder {

    object State extends Enumeration {
      val Header, Encryption, Length, Content = Value
    }

    private var state = State.Header
    private var encryption: Encryption.Value = _
    private var length: Int = _

    private def readContent(cnx: StealthNetConnection, input: InputStream): Command = {
      /* cipher-text section */
      val cipherInput: InputStream = encryption match {
        case Encryption.None =>
          input

        case Encryption.RSA =>
          new CipherInputStream(input, rsaDecrypter(RSAKeys.privateKey))

        case Encryption.Rijndael =>
          new BCCipherInputStream(input, rijndaelDecrypter(cnx.remoteRijndaelParameters))
      }

      val code = ProtocolStream.readByte(cipherInput)
      builders.get(code) match {
        case Some(builder) =>
          builder.read(cipherInput)

        case None =>
          logger error(cnx.loggerContext, "Unknown command code[0x%02X] with encryption[%s]".format(code, encryption))
          /* XXX - take error into account, and drop connection if too many ? */
          null
      }
    }

    def decryptCommand(cnx: StealthNetConnection, cipher: Array[Byte], offset: Int, lenght: Int): Array[Byte] = {
      if (encryption == Encryption.None)
        return cipher

      val input: InputStream = new ByteArrayInputStream(cipher, offset, length)
      val cipherInput: InputStream = encryption match {
        case Encryption.RSA =>
          new CipherInputStream(input, rsaDecrypter(RSAKeys.privateKey))

        case Encryption.Rijndael =>
          new BCCipherInputStream(input, rijndaelDecrypter(cnx.remoteRijndaelParameters))
      }

      val output = new ByteArrayOutputStream()
      val buffer = new Array[Byte](1024)
      var read = 1
      while (read > 0) {
        read = cipherInput.read(buffer)
        if (read > 0)
          output.write(buffer, 0, read)
      }
      output.close()
      output.toByteArray()
    }

    def readCommand(cnx: StealthNetConnection, input: InputStream): Command = {
      assert(state == State.Content)
      val command = readContent(cnx, input)
      state = State.Header
      command
    }

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
        encryption = Encryption.value(encryptionRAW)
        if (encryption == Encryption.Unknown)
          throw new InvalidDataException("Invalid encryption[%02X]".format(encryptionRAW), loggerContext = cnx.loggerContext)
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

abstract class Command extends CommandArguments {

  val code: Byte

  val encryption: Encryption.Value

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
        new CipherOutputStream(output, rsaEncrypter(cnx.remoteRSAKey))

      case Encryption.Rijndael =>
        new BCCipherOutputStream(output, rijndaelEncrypter(cnx.localRijndaelParameters))
    }

    var unencryptedLength: Int = 0
    unencryptedLength += ProtocolStream.writeByte(cipherOutput, code)
    unencryptedLength += write(cipherOutput)
    cipherOutput.flush()
    cipherOutput.close()

    unencryptedLength
  }

}
