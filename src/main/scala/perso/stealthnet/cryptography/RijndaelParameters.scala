package perso.stealthnet.cryptography

import java.security.SecureRandom
import perso.stealthnet.network.protocol.Constants

/**
 * Rijndael class companion object.
 * <p>
 * Note: technically, it is 'Rijndael' (a superset) because 'AES' shall be
 * limited to 128-bits block size.
 */
object RijndaelParameters {

  /**
   * Factory method.
   * Generates new Rijndael parameters with:<ul>
   *   <li>default block size</li>
   *   <li>default feedback size</li>
   *   <li>default key size</li>
   *   <li>default cipher mode</li>
   *   <li>default padding mode</li>
   *   <li>random initialization vector</li>
   *   <li>random key</li>
   * </ul>
   *
   * XXX - are random bytes secure enough for initialization vector and key, or
   * should we use a specific generator ?
   */
  def apply(): RijndaelParameters = {
    val blockSize = Constants.RijndaelBlockSize
    val feedbackSize = Constants.RijndaelFeedbackSize
    val keySize = Constants.RijndaelKeySize
    val cipherMode = Constants.RijndaelCipherMode
    val paddingMode = Constants.RijndaelPaddingMode
    val random = new SecureRandom()
    val iv = new Array[Byte](blockSize / 8)
    val key = new Array[Byte](keySize / 8)
    random.nextBytes(iv)
    random.nextBytes(key)

    new RijndaelParameters(blockSize, feedbackSize, keySize, cipherMode,
      paddingMode, iv, key)
  }

}

/**
 * Rijndael parameters.
 */
class RijndaelParameters(
  val blockSize: Int,
  val feedbackSize: Int,
  val keySize: Int,
  val cipherMode: CipherMode.Value,
  val paddingMode: PaddingMode.Value,
  val iv: Array[Byte],
  val key: Array[Byte]
)
