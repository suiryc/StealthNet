package stealthnet.scala

import stealthnet.scala.cryptography.{PaddingMode, CipherMode}

/**
 * StealthNet constants.
 */
object Constants {

  /** Protocol header. */
  val protocolRAW = "LARS REGENSBURGER'S FILE SHARING PROTOCOL 0.2".getBytes("US-ASCII")

  /** Offset of command length: protocol header + encryption = 45 + 1 = 46 */
  val commandLengthOffset = 46

  /** Offset of command */
  val commandOffset = commandLengthOffset + 2

  /** ''RSA'' key length: 1024-bits. */
  val RSAKeyLength = 1024

  /** ''Rijndael'' block size: 256-bits. */
  val RijndaelBlockSize = 256

  /** ''Rijndael'' feedback size: 256-bits. */
  val RijndaelFeedbackSize = 256

  /** ''Rijndael'' key size: 256-bits. */
  val RijndaelKeySize = 256

  /** ''Rijndael'' cipher mode: ''CBC''. */
  val RijndaelCipherMode = CipherMode.CBC

  /** ''Rijndael'' padding mode: ''PKCS #7''. */
  val RijndaelPaddingMode = PaddingMode.PKCS7

}
