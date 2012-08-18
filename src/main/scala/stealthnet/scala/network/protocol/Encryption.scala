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

  private val None_id: Byte = 0x00
  private val RSA_id: Byte = 0x01
  private val Rijndael_id: Byte = 0x02

  /**
   * Gets the id (protocol value) corresponding to a mode.
   *
   * @param v the mode for which to determine the id
   * @return the corresponding protocol value
   */
  def id(v: Encryption.Value): Byte = v match {
    case None => None_id
    case RSA => RSA_id
    case Rijndael => Rijndael_id
  }

  /**
   * Gets the mode corresponding to an id.
   *
   * @param v the id for which to determine the mode
   * @return an option value containing the corresponding mode,
   *   or `None` if none exists
   */
  def value(v: Byte): Option[Encryption.Value] = v match {
    case None_id => Some(None)
    case RSA_id => Some(RSA)
    case Rijndael_id => Some(Rijndael)
    case _ => scala.None
  }

}
