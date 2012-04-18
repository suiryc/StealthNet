package perso.stealthnet.network.protocol

/**
 * Encryption methods.
 */
object Encryption extends Enumeration {

  /** Available methods. */
  val None, RSA, Rijndael, Unknown = Value

  def id(v: Encryption.Value): Byte = v match {
    case None => 0x00
    case RSA => 0x01
    case Rijndael => 0x02
    case _ => 0xFF.asInstanceOf[Byte]
  }

  def value(v: Byte): Encryption.Value = v match {
    case 0x00 => None
    case 0x01 => RSA
    case 0x02 => Rijndael
    case _ => Unknown
  }

}
