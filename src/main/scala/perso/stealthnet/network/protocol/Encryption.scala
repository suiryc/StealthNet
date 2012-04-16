package perso.stealthnet.network.protocol

/**
 * Encryption methods.
 */
object Encryption extends Enumeration {

  /** Available methods. */
  val None, RSA, Rijndael, Unknown = Value

  def id(v: Encryption.Value): Byte = v match {
    case None => 0x00 byteValue
    case RSA => 0x01 byteValue
    case Rijndael => 0x02 byteValue
    case _ => 0xFF byteValue
  }

  def value(v: Byte): Encryption.Value = v match {
    case 0x00 => None
    case 0x01 => RSA
    case 0x02 => Rijndael
    case _ => Unknown
  }

}
