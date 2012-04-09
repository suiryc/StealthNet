package perso.stealthnet.network.protocol

/**
 * Encryption methods.
 */
object Encryption extends Enumeration {

  /** Available methods. */
  val None, RSA, Rijndael = Value

  def id(v: Encryption.Value): Byte = v match {
    case None => 0x00 byteValue
    case RSA => 0x01 byteValue
    case Rijndael => 0x02 byteValue
  }

}
