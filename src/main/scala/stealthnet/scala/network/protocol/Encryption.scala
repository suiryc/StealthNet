package stealthnet.scala.network.protocol

/**
 * Encryption modes.
 */
object Encryption extends Enumeration {

  /** Available mode: no encryption. */
  val None = Value
  /** Available mode: ''RSA''. */
  val RSA = Value
  /** Available mode: ''Rijndael''. */
  val Rijndael = Value

  /**
   * Gets the id (protocol value) corresponding to a mode.
   *
   * @param v the mode for which to determine the id
   * @return the corresponding protocol value
   */
  def id(v: Encryption.Value): Byte = v match {
    case None => 0x00
    case RSA => 0x01
    case Rijndael => 0x02
  }

  /**
   * Gets the mode corresponding to an id.
   *
   * @param v the id for which to determine the mode
   * @return an option value containing the corresponding mode,
   *   or `None` if none exists
   */
  def value(v: Byte): Option[Encryption.Value] = v match {
    case 0x00 => Some(None)
    case 0x01 => Some(RSA)
    case 0x02 => Some(Rijndael)
    case _ => scala.None
  }

}
