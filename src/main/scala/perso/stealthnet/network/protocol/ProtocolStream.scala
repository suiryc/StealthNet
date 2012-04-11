package perso.stealthnet.network.protocol

import java.io.OutputStream
import perso.stealthnet.core.cryptography.Hash
import java.io.InputStream
import java.io.EOFException

object ProtocolStream {

  /* XXX - since values are unsigned, we need to use more bits */

  def convertShort(value: Int): Array[Byte] = {
    if (value > 0xFFFF)
      throw new IllegalArgumentException("Short value[" + value + "] exceeds capacity")

    Array[Byte](
      (value & 0xFF).byteValue,
      ((value >>> 8) & 0xFF).byteValue
    )
  }

  def convertShort(value: Array[Byte]): Int = {
    (0xFF & value(0).asInstanceOf[Int]) | ((0xFF & value(1).asInstanceOf[Int]) << 8)
  }

  def writeHeader(output: OutputStream): Int = {
    output.write(Constants.protocolRAW)
    Constants.protocolRAW.length
  }

  def readByte(input: InputStream): Byte = {
    val value = input.read()

    if (value < 0)
      throw new EOFException()
    else
      value byteValue
  }

  def writeByte(output: OutputStream, value: Byte): Int = {
    output.write(value)
    1
  }

  def readBytes(input: InputStream): Array[Byte] = {
    val length = readShort(input)
    val bytes = new Array[Byte](length)

    if (input.read(bytes) != length)
      throw new EOFException()
    else
      bytes
  }

  def writeBytes(output: OutputStream, value: Array[Byte]): Int = {
    if (value.length > 0xFFFF)
      throw new IllegalArgumentException("Bytes array length[" + value.length + "] exceeds capacity")

    writeShort(output, value.length)
    output.write(value)
    value.length + 2
  }

  def readShort(input: InputStream): Int = {
    var value: Int = 0
    for (idx <- 0 to 1)
      value |= (0xFF & readByte(input).asInstanceOf[Int]) << (8 * idx)
    value
  }

  def writeShort(output: OutputStream, value: Int): Int = {
    if (value > 0xFFFF)
      throw new IllegalArgumentException("Short value[" + value + "] exceeds capacity")

    for (idx <- 0 to 1)
      output.write((value >>> (8 * idx)) & 0xFF)
    2
  }

  def readString(input: InputStream): String = {
    val length = readShort(input)
    val bytes = new Array[Byte](length)

    if (input.read(bytes) != length)
      throw new EOFException()
    else
      new String(bytes, "UTF-8")
  }

  def writeString(output: OutputStream, value: String): Int = {
    val bytes = value.getBytes("UTF-8")

    if (bytes.length > 0xFFFF)
      throw new IllegalArgumentException("String[%s] length exceeds capacity".format(value))

    writeShort(output, bytes.length)
    output.write(bytes)
    bytes.length + 2
  }

  def readHash(input: InputStream): Hash = {
    /* shall be a SHA384 hash */
    val length = 48
    val bytes = new Array[Byte](length)

    if (input.read(bytes) != length)
      throw new EOFException()
    else
      bytes
  }

  def writeHash(output: OutputStream, value: Hash): Int = {
    /* shall be a SHA384 hash */
    assert(value.bytes.length == 48)

    output.write(value.bytes)
    value.bytes.length
  }

  def write(output: OutputStream, value: Any): Int = {
    value match {
      case v: Byte => writeByte(output, v)
      case v: Array[Byte] => writeBytes(output, v)
      case v: Int => writeShort(output, v)
      case v: String => writeString(output, v)
      case v: Hash => writeHash(output, v)
      case _ => 0
    }
  }

}
