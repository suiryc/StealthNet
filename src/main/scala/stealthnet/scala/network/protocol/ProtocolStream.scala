package stealthnet.scala.network.protocol

import java.io.{InputStream, EOFException, OutputStream}
import java.math.BigInteger
import stealthnet.scala.Constants
import stealthnet.scala.network.protocol.commands.CommandArguments
import stealthnet.scala.network.protocol.exceptions.InvalidDataException

/**
 * Protocol data bit size.
 */
object BitSize {
  /** Byte is 8-bits. */
  val Byte = 8
  /** Short is 16-bits. */
  val Short = 16
  /** Int is 32-bits. */
  val Int = 32
}

/**
 * Protocol stream helper methods.
 *
 * @note ''StealthNet'' protocol integers are written in little-endian order.
 *   This does not apply to big integers (used in ''RSA'' key modulus and
 *   exponent) which are written in big-endian order (as does ''C#'' - on which
 *   rely original ''StealthNet'' application - represent them).
 * @note ''StealthNet'' protocol integers (byte, short and int) are unsigned
 *   values. Since ''Scala'' (as in ''Java'') only manipulates signed values, it
 *   is often necessary to use a larger integer type to hold and correctly use
 *   them.
 */
object ProtocolStream {

  /**
   * Converts an integer to its corresponding byte array.
   *
   * @param value value to convert
   * @param bitSize value bit size
   * @return corresponding byte array
   */
  def convertInteger(value: Long, bitSize: Int): Array[Byte] = {
    if (value > (-1L >>> (64 - bitSize)))
      throw new IllegalArgumentException(s"Number value[$value] exceeds capacity")

    if (bitSize == 8)
      Array[Byte](value.asInstanceOf[Byte])
    else
      (for (idx <- 0 until (bitSize / 8))
        yield ((value >>> (8 * idx)) & 0xFF).asInstanceOf[Byte]).toArray
  }

  /**
   * Writes protocol header.
   *
   * @param output stream to write to
   * @return number of written bytes
   */
  def writeHeader(output: OutputStream): Int = {
    output.write(Constants.protocolRAW)
    Constants.protocolRAW.length
  }

  /**
   * Reads bytes.
   *
   * Reads until the requested number of bytes or ''EOF'' is reached.
   *
   * @param input stream to read from
   * @param buffer array to store bytes to
   * @param offset array offset
   * @param length number of bytes to read
   * @return number of read bytes
   */
  private def _read(input: InputStream, buffer: Array[Byte], offset: Int,
    length: Int): Int =
  {
    @annotation.tailrec
    def _read(offset: Int, length: Int, acc: Int): Int = {
      if (length <= 0) acc
      else {
        val read = input.read(buffer, offset, length)
        if (read < 0) acc
        else _read(offset + read, length - read, acc + read)
      }
    }

    _read(offset, length, 0)
  }

  /**
   * Reads a byte.
   *
   * @param input stream to read from
   * @return read value
   * @throws EOFException if ''EOF'' was reached before reading value
   */
  def readByte(input: InputStream): Byte = {
    val value = input.read()

    if (value < 0)
      throw new EOFException()
    else
      value.asInstanceOf[Byte]
  }

  /**
   * Writes a byte.
   *
   * @param output stream to write to
   * @param value value to write
   * @return number of written bytes
   */
  def writeByte(output: OutputStream, value: Byte): Int = {
    output.write(value)
    1
  }

  /**
   * Reads a bytes array.
   *
   * @param input stream to read from
   * @param bitSize array length bit size
   * @return read value
   * @throws EOFException if ''EOF'' was reached before reading value
   */
  def readBytes(input: InputStream, bitSize: Int): Array[Byte] = {
    val length = readInteger(input, bitSize).intValue
    val bytes = new Array[Byte](length)

    if (_read(input, bytes, 0, length) != length)
      throw new EOFException()
    else
      bytes
  }

  /**
   * Writes a bytes array.
   *
   * Writes the array length before then the array content.
   *
   * @param output stream to write to
   * @param value value to write
   * @param bitSize array length bit size
   * @return number of written bytes
   */
  def writeBytes(output: OutputStream, value: Array[Byte], bitSize: Int): Int =
  {
    val sizeLength = writeInteger(output, value.length, bitSize)
    output.write(value)
    value.length + sizeLength
  }

  /**
   * Reads an integer.
   *
   * @param input stream to read from
   * @param bitSize array length bit size
   * @return read value
   * @throws EOFException if ''EOF'' was reached before reading value
   */
  def readInteger(input: InputStream, bitSize: Int): Long = {
    val idxMax = bitSize / 8
    @annotation.tailrec
    def readInteger(idx: Int, acc: Long): Long = if (idx >= idxMax) acc
      else readInteger(idx + 1,
        acc | ((0xFF & readByte(input).asInstanceOf[Long]) << (8 * idx)))

    readInteger(0, 0)
  }

  /**
   * Writes an integer.
   *
   * @param output stream to write to
   * @param value value to write
   * @param bitSize array length bit size
   * @return number of written bytes
   */
  def writeInteger(output: OutputStream, value: Long, bitSize: Int): Int = {
    output.write(convertInteger(value, bitSize))
    bitSize / 8
  }

  /**
   * Reads a big array.
   *
   * @param input stream to read from
   * @return read value
   * @throws EOFException if ''EOF'' was reached before reading value
   * @note Since original protocol deals with unsigned integers, we prepends a
   *   leading sign byte if necessary
   */
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

  /**
   * Writes a big integer.
   *
   * The big integer is converted to an array which is then written (short bit
   * size).
   *
   * @param output stream to write to
   * @param value value to write
   * @param bitSize array length bit size
   * @return number of written bytes
   * @note Since original protocol deals with unsigned integers, we strip the
   *   the big integer leading sign byte if any.
   */
  def writeBigInteger(output: OutputStream, value: BigInt): Int = {
    /* Note: big-endian byte array */
    val representation = value.toByteArray
    val bytes = if ((representation(0) == 0) && (representation.length > 1))
        representation.tail
      else
        representation
    writeBytes(output, bytes, BitSize.Short)
  }

  /**
   * Reads a string.
   *
   * @param input stream to read from
   * @return read value
   * @throws EOFException if ''EOF'' was reached before reading value
   */
  def readString(input: InputStream): String = {
    val length = readInteger(input, BitSize.Short).intValue
    val bytes = new Array[Byte](length)

    if (_read(input, bytes, 0, length) != length)
      throw new EOFException()
    else
      new String(bytes, "UTF-8")
  }

  /**
   * Writes a string.
   *
   * The string is converted to its ''UTF-8'' bytes array representation which
   * is then written (short bit size).
   *
   * @param output stream to write to
   * @param value value to write
   * @return number of written bytes
   */
  def writeString(output: OutputStream, value: String): Int = {
    val bytes = value.getBytes("UTF-8")

    if (bytes.length > 0xFFFF)
      throw new IllegalArgumentException("String[%s] length exceeds capacity".format(value))

    writeBytes(output, bytes, BitSize.Short)
  }

  /**
   * Reads RAW bytes.
   *
   * @param input stream to read from
   * @param length number of bytes to read
   * @return read value
   * @throws EOFException if ''EOF'' was reached before reading value
   */
  def read(input: InputStream, length: Int): Array[Byte] = {
    val bytes = new Array[Byte](length)

    if (_read(input, bytes, 0, length) != length)
      throw new EOFException()
    else
      bytes
  }

  /**
   * Writes RAW bytes.
   *
   * @param output stream to write to
   * @param value value to write
   * @return number of written bytes
   */
  def write(output: OutputStream, value: Array[Byte]): Int = {
    output.write(value)
    value.length
  }

  /**
   * Writes command arguments.
   *
   * @param output stream to write to
   * @param value value to write
   * @return number of written bytes
   * @see [[stealthnet.scala.network.protocol.commands.CommandArguments]].`write`
   */
  def write(output: OutputStream, value: CommandArguments): Int =
    value.write(output)

}
