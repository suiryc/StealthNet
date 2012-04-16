package perso.stealthnet.network.protocol

import java.io.{InputStream, OutputStream}
import javax.crypto.{Cipher, CipherInputStream, CipherOutputStream}
import perso.stealthnet.core.cryptography.{
  Algorithm,
  Hash,
  Message,
  RSAKeys
}
import perso.stealthnet.core.cryptography.Ciphers._
import perso.stealthnet.core.util.{HexDumper, UUID}
import perso.stealthnet.network.{StealthNetConnection, StealthNetConnections}
import perso.stealthnet.core.cryptography.io.{BCCipherInputStream, BCCipherOutputStream}
import perso.stealthnet.core.util.{EmptyLoggingContext, Logging}

trait CommandBuilder {

  val code: Byte

  def read(input: InputStream): Command

}

object Command extends Logging with EmptyLoggingContext {

  private val builders: Map[Byte, CommandBuilder] =
    Map() ++ List[CommandBuilder](RSAParametersServerCommand, RSAParametersClientCommand,
        RijndaelParametersServerCommand, RijndaelParametersClientCommand,
        SearchCommand).map(c => (c.code, c))

  def generateId(): Hash = Message.hash(UUID.generate().bytes, Algorithm.SHA384)

  class Builder {

    object State extends Enumeration {
      val Header, Encryption, Length, Content = Value
    }

    private var state = State.Header
    private var encryption: Encryption.Value = _
    private var length: Int = _

    private def readHeader(input: InputStream) {
      val header = new String(ProtocolStream.read(input, Constants.protocolRAW.length), "US-ASCII")
      /* XXX - cleanly handle issues */
      if (header != Constants.protocol)
        throw new Exception("Invalid protocol header[" + header + "]")
    }

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
          logger error(cnx.loggerContext, "Unknown command code[0x%02X]".format(code))
          /* XXX - take error into account, and drop connection if too many ? */
          null
      }
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
        readHeader(input)
        state = State.Encryption
        -1

      case State.Encryption =>
        encryption = Encryption.value(ProtocolStream.readByte(input))
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

abstract class Command extends Logging with EmptyLoggingContext {

  val code: Byte

  val encryption: Encryption.Value

  def arguments(): List[(String, Any)]

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
    for (tuple <- arguments) {
      val argument = tuple._2
      /* XXX - really necessary ? (most, if not all, commands check ctor arguments) */
      if (argument == null) {
        logger error(cnx.loggerContext, "Missing command argument[" + tuple._1 + "] in " + this)
        return -1
      }

      unencryptedLength += ProtocolStream.write(cipherOutput, argument)
    }
    cipherOutput.flush()
    cipherOutput.close()

    unencryptedLength
  }

  override def toString =
    getClass.getSimpleName + arguments.map(tuple =>
      tuple._1 + "=" + (tuple._2 match {
      case v: Array[Byte] => "\n" + HexDumper.dump(v) + "\n"
      case v => v
    }))

}
