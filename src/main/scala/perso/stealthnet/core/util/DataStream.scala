package perso.stealthnet.core.util

import java.io.ByteArrayOutputStream

import perso.stealthnet.core.cryptography.Hash

/* XXX - shall not be necessary anymore */
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

    if (bytes.length > 0xFFFF)
      throw new IllegalArgumentException("String[%s] length exceeds capacity".format(str))

    write(bytes.length.shortValue)
    baos.write(bytes)
    this
  }

  /* XXX - really useful (other than for protocol header) ? */
  def writeAscii(str: String): DataStream = {
    val bytes = str.getBytes("US-ASCII")

    if (bytes.length > 0xFFFF)
      throw new IllegalArgumentException("String[%s] length exceeds capacity".format(str))

    write(bytes.length.shortValue)
    baos.write(bytes)
    this
  }

  def write(hash: Hash): DataStream = {
    baos.write(hash.bytes)
    this
  }

  def write(data: Any): DataStream = {
    data match {
      case v: Byte => write(v)
      case v: Short => write(v)
      case v: Int => write(v)
      case v: String => write(v)
      case v: Hash => write(v)
      case v if (v != null) =>  write(v.toString())
      case _ =>
    }
    this
  }

  def getBytes(): Array[Byte] = baos.toByteArray

}
