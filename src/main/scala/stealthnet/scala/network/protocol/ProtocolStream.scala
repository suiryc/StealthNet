package stealthnet.scala.network.protocol

import java.io.{InputStream, EOFException, OutputStream}
import java.math.BigInteger
import stealthnet.scala.network.protocol.commands.CommandArgument
import stealthnet.scala.network.protocol.exceptions.InvalidDataException

object BitSize {
  val Byte = 8
  val Short = 16
  val Int = 32
}

object ProtocolStream {

  /* XXX - since values are unsigned, we need to use more bits */

  def convertInteger(value: Long, bitSize: Int): Array[Byte] = {
    if (value > (-1L >>> (64 - bitSize)))
      throw new IllegalArgumentException("Number value[" + value + "] exceeds capacity")

    if (bitSize == 8)
      Array[Byte](value.asInstanceOf[Byte])
    else
      (for (idx <- 0 until (bitSize / 8))
        yield ((value >>> (8 * idx)) & 0xFF).asInstanceOf[Byte]).toArray
  }

  def writeHeader(output: OutputStream): Int = {
    output.write(Constants.protocolRAW)
    Constants.protocolRAW.length
  }

  private def _read(input: InputStream, buffer: Array[Byte], offset: Int,
      length: Int): Int =
  {
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
      value.asInstanceOf[Byte]
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

  def writeBytes(output: OutputStream, value: Array[Byte], bitSize: Int): Int =
  {
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
    output.write(convertInteger(value, bitSize))
    bitSize / 8
  }

  def readBigInteger(input: InputStream): BigInt = {
    val length = readInteger(input, BitSize.Short).intValue
    val initial = readByte(input)
    val offset = if (initial < 0) 1 else 0
    val bytes = new Array[Byte](length + offset)
    bytes(offset) = initial

    if (_read(input, bytes, offset + 1, length - 1) != length - 1)
      throw new EOFException()
    else
      BigInt(bytes)
  }

  def writeBigInteger(output: OutputStream, value: BigInt): Int = {
    /* Note: big-endian byte array */
    val representation = value.toByteArray
    /* Scala's BigInt may have a leading 0x00 byte because it is the sign
     * value, while we expect an unsigned value in protocol; so strip it */
    val bytes = if ((representation(0) == 0) && (representation.length > 1))
        representation.tail
      else
        representation
    writeBytes(output, bytes, BitSize.Short)
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

  def read(input: InputStream, length: Int): Array[Byte] = {
    val bytes = new Array[Byte](length)

    if (_read(input, bytes, 0, length) != length)
      throw new EOFException()
    else
      bytes
  }

  def write(output: OutputStream, value: Array[Byte]): Int = {
    output.write(value)
    value.length
  }

  def write(output: OutputStream, value: CommandArgument): Int =
    value.write(output)

}
