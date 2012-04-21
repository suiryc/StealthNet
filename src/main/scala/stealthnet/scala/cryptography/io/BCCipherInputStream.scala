package stealthnet.scala.cryptography.io

import java.io.{FilterInputStream, InputStream}
import org.bouncycastle.crypto.BufferedBlockCipher

/**
 * Input stream filter decrypting data using ''BouncyCastle''.
 *
 * @note ''BouncyCastle'' provides `CipherInputStream` which may not be
 * optimal with some kind of `InputStream` (may read one byte at a time).
 */
class BCCipherInputStream(input: InputStream, cipher: BufferedBlockCipher)
  extends FilterInputStream(input)
{

  protected var inputBuffer: Array[Byte] = new Array[Byte](1024)
  protected var buffer: Array[Byte] = new Array[Byte](1024)
  protected var bufferLength = 0
  protected var bufferOffset = 0
  protected var finalized = false

  protected def fillBuffer(last: Boolean): Int = {
    if (finalized)
      return -1

    var doLast = last
    val read = input.read(inputBuffer)
    if (read == -1)
      doLast = true

    val length = if (doLast)
        cipher.getOutputSize(if (read > 0) read else 0)
      else
        cipher.getUpdateOutputSize(read)

    if (length > buffer.length)
      buffer = new Array[Byte](length)

    bufferOffset = 0
    bufferLength = if (read > 0)
        cipher.processBytes(inputBuffer, 0, read, buffer, 0)
      else
        0

    if (doLast) {
      bufferLength += cipher.doFinal(buffer, bufferLength)
      finalized = true
    }

    if (finalized && (bufferLength == 0))
      -1
    else
      bufferLength
  }

  override def available(): Int = bufferLength - bufferOffset

  /**
   * @todo test suite to ensure that 0x80-0xFF bytes are correctly read as
   * positive integers (previously fixed bug)
   */
  override def read(): Int = {
    while (available == 0) {
      if (fillBuffer(false) == -1)
        return -1
    }

    bufferOffset += 1
    /* Note: make sure to return the byte value in the range 0-255 as needed */
    0xFF & buffer(bufferOffset - 1)
  }

  override def read(b: Array[Byte]) = read(b, 0, b.length)

  /**
   * @todo shall we check parameters (as does parent class) ?
   */
  override def read(b: Array[Byte], off: Int, len: Int): Int = {
    if (len <= 0)
      return 0

    while (available == 0) {
      if (fillBuffer(false) == -1)
        return -1
    }

    val length = if (len < available) len else available
    System.arraycopy(buffer, bufferOffset, b, off, length)
    bufferOffset += length

    length
  }

  /**
   * This implementation do `read` if necessary until the requested number
   * of bytes are skipped or `EOF` is reached.
   */
  override def skip(n: Long): Long = {
    if (n <= 0)
      return 0

    var remaining = n
    var stop = false
    val tmp = new Array[Byte](1024)
    while ((remaining > 0) && !stop) {
      val length = read(tmp, 0, if (remaining < tmp.length) remaining.intValue else tmp.length)
      if (length == -1)
        stop = true
      else
        remaining -= length
    }

    n - remaining
  }

  /**
   * This implementation does `reset` the cipher.
   */
  override def close() {
    cipher.reset()
    super.close()
  }

  /**
   * This input stream does not support `mark` and `reset` methods.
   *
   * @return `false`
   */
  override def markSupported(): Boolean = false

}
