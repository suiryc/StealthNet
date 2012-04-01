package perso.stealthnet.sandbox

import java.io.ByteArrayOutputStream

import perso.stealthnet.core.cryptography.Hash
import perso.stealthnet.core.util.UUID

/**
 * Encryption methods.
 */
object Encryption extends Enumeration {

  /** Available methods. */
  val None, RSA, Rijndael = Value

  def id(v: Encryption.Value): Byte = v match {
    case None => 0x00 byteValue
    case RSA => 0x01 byteValue
    case Rijndael => 0x02 byteValue
  }

}

object Constants {

  val protocol = "LARS REGENSBURGER'S FILE SHARING PROTOCOL 0.2";

}

class SearchCommand {

  def process() {
    val commandId: Hash = UUID.generate()
    val floodingHash: Hash = generateFloodingHash()
    val senderPeerID: Hash = UUID.generate()
    val searchID: Hash = UUID.generate()
    val searchPattern: String = "intouchables"

  }

  def build(commandId: Hash, floodingHash: Hash, senderPeerID: Hash, searchID: Hash, searchPattern: String) {
    val dataStream = (new DataStream)
        .write(0x20 byteValue)
        .write(commandId)
        .write(floodingHash)
        .write(senderPeerID)
        .write(searchID)
        .write(searchPattern)

    val commandBuilder = new CommandBuilder(Encryption.Rijndael, 0x20 byteValue, dataStream)
  }

  /* http://www.scribd.com/doc/28681327/69/Stealthnet-decloaked */
  def generateFloodingHash(): Hash = {
    var hash: Hash = UUID.generate()

    while (hash.bytes(47) <= 51) {
      hash = UUID.generate()
    }

    hash
  }

}

class DataStream {

  private val baos = new ByteArrayOutputStream()

  def write(b: Byte): DataStream = {
    baos.write(b)
    this
  }

  def write(s: Short): DataStream = {
    for (idx <- 0 to 1)
      baos.write(((s >>> (8 * idx)) & 0xFF).byteValue)
    this
  }

  def write(i: Int): DataStream = {
    for (idx <- 0 to 3)
      baos.write(((i >>> (8 * idx)) & 0xFF).byteValue)
    this
  }

  def write(str: String): DataStream = {
    val bytes = str.getBytes("UTF-8")

    if ((bytes.length & 0xFFFF) != 0)
      throw new IllegalArgumentException("String[%s] length exceeds capacity".format(str))

    baos.write(bytes.length.shortValue)
    baos.write(bytes)
    this
  }

  /* XXX - really useful (other than for procotol header) ? */
  def writeAscii(str: String): DataStream = {
    val bytes = str.getBytes("US-ASCII")

    if ((bytes.length & 0xFFFF) != 0)
      throw new IllegalArgumentException("String[%s] length exceeds capacity".format(str))

    baos.write(bytes.length.shortValue)
    baos.write(bytes)
    this
  }

  def write(hash: Hash): DataStream = {
    baos.write(hash.bytes)
    this
  }

  def getBytes(): Array[Byte] = baos.toByteArray

}

class CommandBuilder(val encryption: Encryption.Value, val code: Byte, val dataStream: DataStream) {

  def send(/* connection */) = {
    /* XXX */
    val data: Array[Byte] = encryption match {
      case Encryption.None => dataStream.getBytes
      case Encryption.RSA => dataStream.getBytes
      /* encryptedData = Core.RSAEncrypt(connection.PublicKey, m_CommandData.ToArray()); */
      case Encryption.Rijndael => dataStream.getBytes
      /* encryptedData = Core.RijndaelEncrypt(connection.SendingKey, m_CommandData.ToArray()); */
    }

    if ((data.length & 0xFFFF) != 0)
      throw new IllegalArgumentException("Command[%d] length[%d] exceeds capacity".format(code.intValue, data.length))

    val command = (new DataStream)
        .writeAscii(Constants.protocol)
        .write(Encryption.id(encryption))
        .write(data.length.shortValue)
        .write(data)

    /* connection.send(command.getBytes(), code) */
  }

}
