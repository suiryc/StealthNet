package stealthnet.scala

import stealthnet.scala.cryptography.{PaddingMode, CipherMode}

/**
 * StealthNet constants.
 */
object Constants {

  /** Protocol header. */
  val protocolRAW: Array[Byte] = "LARS REGENSBURGER'S FILE SHARING PROTOCOL 0.2".getBytes("US-ASCII")

  /** Offset of command length: protocol header + encryption = 45 + 1 = 46 */
  val commandLengthOffset = 46

  /** Offset of command */
  val commandOffset: Int = commandLengthOffset + 2

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

  /** Buffer length used when encrypting/decrypting. */
  val cipherBufferLength = 1024

  /* XXX - find better name ? */
  /** Hash length: 48 bytes. */
  val hashLength_48B = 48

  /** Hash length: 64 bytes. */
  val hashLength_64B = 64

  /** Minimal time (ms) between two peer requests. */
  val peerRequestPeriod = 5000

  /** Time (ms) between checks when a peer request is ongoing. */
  val peerRequestCheckPeriod = 2000

  /** Buffer length when sending a command. */
  val commandOutputBufferLength = 512

}
