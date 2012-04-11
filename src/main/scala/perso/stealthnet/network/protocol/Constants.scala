package perso.stealthnet.network.protocol

object Constants {

  val protocol = "LARS REGENSBURGER'S FILE SHARING PROTOCOL 0.2"
  val protocolRAW = protocol.getBytes("US-ASCII")

  /* protocol header length + encryption = 45 + 1 = 46 */
  val commandLengthOffset = 46

  val RSAKeyLength = 1024

}
