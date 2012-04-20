package perso.stealthnet.cryptography.io

import java.io.{FilterOutputStream, OutputStream}
import org.bouncycastle.crypto.BufferedBlockCipher

/**
 * Output stream filter encrypting data using <i>BouncyCastle</i>.
 * <p>
 * Note: <i>BouncyCastle</i>'s <tt>CipherOutputStream</tt> may be sufficient,
 * but stay consistent with the fact we have <tt>BCCipherInputStream</tt>.
 */
class BCCipherOutputStream(output: OutputStream, cipher: BufferedBlockCipher)
  extends FilterOutputStream(output)
{

  protected var buffer = new Array[Byte](1024)

  /**
   * {@inheritDoc}
   */
  override def write(b: Int) {
    var length = cipher.getUpdateOutputSize(1)

    if (length > buffer.length)
      buffer = new Array[Byte](length)

    length = cipher.processByte(b.asInstanceOf[Byte], buffer, 0)
    if (length > 0)
      output.write(buffer, 0, length)
  }

  /**
   * {@inheritDoc}
   */
  override def write(b: Array[Byte]) = write(b, 0, b.length)

  /**
   * {@inheritDoc}
   *
   * XXX - shall we check parameters (as does parent class) ?
   */
  override def write(b: Array[Byte], off: Int, len: Int) {
    var length = cipher.getUpdateOutputSize(len)

    if (length > buffer.length)
      buffer = new Array[Byte](length)

    length = cipher.processBytes(b, off, len, buffer, 0)
    if (length > 0)
      output.write(buffer, 0, length)
  }

  /**
   * {@inheritDoc}
   * <p>
   * This implementation does <tt>reset</tt> the cipher.
   */
  override def close() {
    var length = cipher.getOutputSize(0)

    if (length > buffer.length)
      buffer = new Array[Byte](length)

    length = cipher.doFinal(buffer, 0)
    if (length > 0)
      output.write(buffer, 0, length)

    cipher.reset()
    /* Note: parent class shall actually do it too */
    flush()

    super.close()
  }

}
