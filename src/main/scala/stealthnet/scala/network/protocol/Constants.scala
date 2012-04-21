package stealthnet.scala.network.protocol

import stealthnet.scala.cryptography.{PaddingMode, CipherMode}

object Constants {

  val protocol = "LARS REGENSBURGER'S FILE SHARING PROTOCOL 0.2"
  val protocolRAW = protocol.getBytes("US-ASCII")

  /* protocol header length + encryption = 45 + 1 = 46 */
  val commandLengthOffset = 46

  val commandOffset = commandLengthOffset + 2

  val RSAKeyLength = 1024

  val RijndaelBlockSize = 256

  val RijndaelFeedbackSize = 256

  val RijndaelKeySize = 256

  val RijndaelCipherMode = CipherMode.CBC

  val RijndaelPaddingMode = PaddingMode.PKCS7

}
