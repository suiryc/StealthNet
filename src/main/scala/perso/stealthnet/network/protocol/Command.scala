package perso.stealthnet.network.protocol

import java.io.{InputStream, OutputStream}
import javax.crypto.{Cipher, CipherOutputStream}
import com.weiglewilczek.slf4s.Logging
import perso.stealthnet.core.cryptography.{
  Algorithm,
  Hash,
  Message
}
import perso.stealthnet.core.util.UUID

trait CommandBuilder {

  val code: Byte

  def read(input: InputStream): Command

}

object Command extends Logging {

  private val builders: List[CommandBuilder] =
    List(RSAParametersCommand, SearchCommand)

  def generateId(): Hash = Message.hash(UUID.generate().bytes, Algorithm.SHA384)

  def read(input: InputStream): Command = {
    val code = ProtocolStream.readByte(input)

    val command = builders.find(_.code == code) match {
      case Some(builder) =>
        val result = builder.read(input)
        if (input.read() != -1) {
          logger error("Command[%s] is followed by unexpected data".format(result))
          null
        }
        else
          result

      case None =>
        logger error("Unknown command code[0x%02X]".format(code))
        null
    }

    command
  }

}

abstract class Command(val code: Byte, val encryption: Encryption.Value)
  extends Logging
{

  def arguments(): List[Any]

  def write(output: OutputStream): Int = {
    /* plain-text section */
    ProtocolStream.writeAscii(output, Constants.protocol)
    ProtocolStream.writeByte(output, Encryption.id(encryption))
    ProtocolStream.writeShort(output, 0)
    output.flush()

    /* cipher-text section */
    /* XXX - get writing cipher of connection */
    val cipher: Cipher = encryption match {
      case Encryption.None => null
      case Encryption.RSA => null
      /* encryptedData = Core.RSAEncrypt(connection.PublicKey, m_CommandData.ToArray()); */
      case Encryption.Rijndael => null
      /* encryptedData = Core.RijndaelEncrypt(connection.SendingKey, m_CommandData.ToArray()); */
    }
    val cipherOutput = if (cipher != null)
        new CipherOutputStream(output, cipher)
      else
        output
    var cipherLength: Int = 0

    cipherLength += ProtocolStream.writeByte(cipherOutput, code)
    for (argument <- arguments) {
      /* XXX - really necessary ? (most, if not all, commands check ctor arguments) */
      if (argument == null) {
        logger error("Missing command argument in " + this)
        return -1
      }

      cipherLength += ProtocolStream.write(cipherOutput, argument)
    }
    cipherOutput.flush()
    cipherOutput.close()

    if (cipherLength > 0xFFFF) {
      logger error("Command[%s] length exceeds capacity".format(this))
      return -1
    }

    cipherLength
  }

  override def toString =
    getClass.getSimpleName + arguments.map(_ match {
      case v: Array[Byte] => "0x" + Hash.bytesToHash(v)
      case v => v
    })

}
