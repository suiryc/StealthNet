package perso.stealthnet.core.cryptography.io

import java.io.{FilterOutputStream, OutputStream}
import org.bouncycastle.crypto.BufferedBlockCipher

class BCCipherOutputStream(output: OutputStream, cipher: BufferedBlockCipher)
  extends FilterOutputStream(output)
{

  private var buffer = new Array[Byte](1024)

  override def write(b: Int) {
    var length = cipher.getUpdateOutputSize(1)

    if (length > buffer.length)
      buffer = new Array[Byte](length)

    length = cipher.processByte(b.asInstanceOf[Byte], buffer, 0)
    if (length > 0)
      output.write(buffer, 0, length)
  }

  /* Note: parent class shall actually do the same */
  override def write(b: Array[Byte]) = write(b, 0, b.length)

  override def write(b: Array[Byte], off: Int, len: Int) {
    /* XXX - shall we check parameters (as does parent class) ? */
    var length = cipher.getUpdateOutputSize(len)

    if (length > buffer.length)
      buffer = new Array[Byte](length)

    length = cipher.processBytes(b, off, len, buffer, 0)
    if (length > 0)
      output.write(buffer, 0, length)
  }

  override def close() {
    var length = cipher.getOutputSize(0)

    if (length > buffer.length)
      buffer = new Array[Byte](length)

    length = cipher.doFinal(buffer, 0)
    if (length > 0)
      output.write(buffer, 0, length)

    /* Note: parent class shall actually do it too */
    flush()

    super.close()
  }

}
