package perso.stealthnet.network.protocol

import java.io.OutputStream
import perso.stealthnet.core.cryptography.Hash
import java.io.InputStream
import java.io.EOFException

object BitSize {
  val Byte = 8
  val Short = 16
}

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

  /*
  def convertShort(value: Array[Byte]): Int = {
    (0xFF & value(0).asInstanceOf[Int]) | ((0xFF & value(1).asInstanceOf[Int]) << 8)
  }
  */

  def writeHeader(output: OutputStream): Int = {
    output.write(Constants.protocolRAW)
    Constants.protocolRAW.length
  }

  private def _read(input: InputStream, buffer: Array[Byte], offset: Int, length: Int): Int =  {
    var actual = 0

    while (actual < length) {
      val read = input.read(buffer, offset + actual, length - actual)
      if (read < 0)
        return actual
      actual += read
    }

    actual
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

  def readBytes(input: InputStream, bitSize: Int): Array[Byte] = {
    val length = readInteger(input, bitSize).intValue
    val bytes = new Array[Byte](length)

    if (_read(input, bytes, 0, length) != length)
      throw new EOFException()
    else
      bytes
  }

  def writeBytes(output: OutputStream, value: Array[Byte], bitSize: Int): Int = {
    val sizeLength = writeInteger(output, value.length, bitSize)
    output.write(value)
    value.length + sizeLength
  }

  def readInteger(input: InputStream, bitSize: Int): Long = {
    var value: Long = 0
    for (idx <- 0 until (bitSize / 8))
      value |= (0xFF & readByte(input).asInstanceOf[Long]) << (8 * idx)
    value
  }

  def writeInteger(output: OutputStream, value: Long, bitSize: Int): Int = {
    if (value > (1L << (bitSize - 1)))
      throw new IllegalArgumentException("Number value[" + value + "] exceeds capacity")

    for (idx <- 0 until (bitSize / 8))
      output.write(((value >>> (8 * idx)) & 0xFF).intValue)
    bitSize / 8
  }

  def readString(input: InputStream): String = {
    val length = readInteger(input, BitSize.Short).intValue
    val bytes = new Array[Byte](length)

    if (_read(input, bytes, 0, length) != length)
      throw new EOFException()
    else
      new String(bytes, "UTF-8")
  }

  def writeString(output: OutputStream, value: String): Int = {
    val bytes = value.getBytes("UTF-8")

    if (bytes.length > 0xFFFF)
      throw new IllegalArgumentException("String[%s] length exceeds capacity".format(value))

    writeInteger(output, bytes.length, BitSize.Short)
    output.write(bytes)
    bytes.length + 2
  }

  def readHash(input: InputStream): Hash = {
    /* shall be a SHA384 hash */
    val length = 48
    val bytes = new Array[Byte](length)

    if (_read(input, bytes, 0, length) != length)
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

  def read(input: InputStream, length: Int): Array[Byte] = {
    val bytes = new Array[Byte](length)

    if (_read(input, bytes, 0, length) != length)
      throw new EOFException()
    else
      bytes
  }

  def write(output: OutputStream, value: Any): Int = {
    value match {
      case v: CommandArgument => v.write(output)
      case v: Byte => writeByte(output, v)
      /* XXX - remove (replace by ByteArrayArgument) ? */
      case v: Array[Byte] => writeBytes(output, v, BitSize.Short)
      case v: Int => writeInteger(output, v, BitSize.Short)
      case v: String => writeString(output, v)
      case v: Hash => writeHash(output, v)
      case _ => 0
    }
  }

}
