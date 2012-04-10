package perso.stealthnet.network.protocol

object Constants {

  val protocol = "LARS REGENSBURGER'S FILE SHARING PROTOCOL 0.2";

  /* protocol string length + protocol string + encryption = 2 + 45 + 1 = 48 */
  val commandLengthOffset = 48

  val RSAKeyLength = 1024

}
