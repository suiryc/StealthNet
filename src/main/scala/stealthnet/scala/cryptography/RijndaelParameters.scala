package stealthnet.scala.cryptography

import java.security.SecureRandom
import stealthnet.scala.network.protocol.Constants
import stealthnet.scala.util.HexDumper

/**
 * ''Rijndael'' class companion object.
 *
 * Note: technically, it is ''Rijndael'' (a superset) because ''AES'' shall be
 * limited to 128-bits block size.
 */
object RijndaelParameters {

  /**
   * Factory method.
   *
   * Generates new ''Rijndael'' parameters with:
   *   - default block size
   *   - default feedback size
   *   - default key size
   *   - default cipher mode
   *   - default padding mode
   *   - random initialization vector
   *   - random key
   *
   * @todo are random bytes secure enough for initialization vector and key, or
   *   should we use a specific generator ?
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
 * ''Rijndael'' parameters.
 */
class RijndaelParameters(
  val blockSize: Int,
  val feedbackSize: Int,
  val keySize: Int,
  val cipherMode: CipherMode.Value,
  val paddingMode: PaddingMode.Value,
  val iv: Array[Byte],
  val key: Array[Byte]
) {

  override def toString() = getClass.getSimpleName + '(' +
    "blockSize=" + blockSize + ", " +
    "feedbackSize=" + feedbackSize + ", " +
    "keySize=" + keySize + ", " +
    "cipherMode=" + cipherMode + ", " +
    "paddingMode=" + paddingMode + ", " +
    "iv=\n" + HexDumper.dump(iv) + "\n, " +
    "key=\n" + HexDumper.dump(key) + "\n)"

}
