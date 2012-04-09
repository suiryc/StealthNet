package perso.stealthnet.network.protocol

import java.io.OutputStream
import perso.stealthnet.core.cryptography.Hash
import java.io.InputStream
import java.io.EOFException

object ProtocolStream {

  def convertShort(value: Short): Array[Byte] = {
    Array[Byte](
      (value & 0xFF).byteValue,
      ((value >>> 8) & 0xFF).byteValue
    )
  }

  def readByte(input: InputStream): Byte = {
    val value = input.read() byteValue

    if (value < 0)
      throw new EOFException()
    else
      value
  }

  def write(output: OutputStream, value: Byte): Int = {
    output.write(value)
    1
  }

  def readShort(input: InputStream): Short = {
    var value: Short = 0
    for (idx <- 1 to 0 by -1)
      value = (value | (readByte(input) << (8 * idx))).shortValue
    value
  }

  def write(output: OutputStream, value: Short): Int = {
    for (idx <- 0 to 1)
      output.write(((value >>> (8 * idx)) & 0xFF).byteValue)
    2
  }

  def readInt(input: InputStream): Int = {
    var value: Int = 0
    for (idx <- 3 to 0 by -1)
      value = value | (readByte(input) << (8 * idx))
    value
  }

  def write(output: OutputStream, value: Int): Int = {
    for (idx <- 0 to 3)
      output.write(((value >>> (8 * idx)) & 0xFF).byteValue)
    4
  }

  /* XXX - other readXXX functions */
  /* XXX - rename write to writeXXX to be consistent ? */
  def write(output: OutputStream, value: String): Int = {
    val bytes = value.getBytes("UTF-8")

    if (bytes.length > 0xFFFF)
      throw new IllegalArgumentException("String[%s] length exceeds capacity".format(value))

    write(output, bytes.length.shortValue)
    output.write(bytes)
    bytes.length + 2
  }

  /* XXX - really useful (other than for protocol header) ? */
  def writeAscii(output: OutputStream, value: String): Int = {
    val bytes = value.getBytes("US-ASCII")

    if (bytes.length > 0xFFFF)
      throw new IllegalArgumentException("String[%s] length exceeds capacity".format(value))

    write(output, bytes.length.shortValue)
    output.write(bytes)
    bytes.length
  }

  def write(output: OutputStream, value: Hash): Int = {
    output.write(value.bytes)
    value.bytes.length
  }

  def write(output: OutputStream, value: Any): Int = {
    value match {
      case v: Byte => write(output, v)
      case v: Short => write(output, v)
      case v: Int => write(output, v)
      case v: String => write(output, v)
      case v: Hash => write(output, v)
      case v if (v != null) =>  write(output, v.toString())
      case _ => 0
    }
  }

}
