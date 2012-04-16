package perso.stealthnet.core.cryptography

import java.security.SecureRandom
import perso.stealthnet.network.protocol.Constants

/* Technically, it is 'Rijndael' (a superset) because 'AES' shall be limited
 * to 128-bits block size.
 */

object RijndaelParameters {

  def apply(): RijndaelParameters = {
    val blockSize = Constants.RijndaelBlockSize
    val feedbackSize = Constants.RijndaelFeedbackSize
    val keySize = Constants.RijndaelKeySize
    val cipherMode = Constants.RijndaelCipherMode
    val paddingMode = Constants.RijndaelPaddingMode
    /* XXX - secure enough, or should we used a specific generator ? */
    val random = new SecureRandom()
    val key = new Array[Byte](keySize / 8)
    val iv = new Array[Byte](blockSize / 8)
    random.nextBytes(key)
    random.nextBytes(iv)

    new RijndaelParameters(blockSize, feedbackSize, keySize, cipherMode, paddingMode, key, iv)
  }

}

class RijndaelParameters(
  val blockSize: Int,
  val feedbackSize: Int,
  val keySize: Int,
  val cipherMode: CipherMode.Value,
  val paddingMode: PaddingMode.Value,
  val key: Array[Byte],
  val iv: Array[Byte]
)
