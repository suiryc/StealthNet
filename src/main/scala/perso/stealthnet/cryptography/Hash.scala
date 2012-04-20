package perso.stealthnet.cryptography

/**
 * Hash class companion object.
 */
object Hash {

  /** Factory method from bytes array. */
  def apply(bytes: Array[Byte]): Hash = {
    require(bytes != null)

    new Hash(bytes)
  }

  /** Factory method from hexadecimal string. */
  def apply(hex: String): Hash = {
    require(hex != null)

    val actual: String = if (hex.length % 2 == 1)
      "0" + hex
    else
      hex

    Hash(actual.grouped(2).map(Integer.parseInt(_, 16).asInstanceOf[Byte]).toArray)
  }

  /** Implicit conversion from hexadecimal string. */
  implicit def hexToHash(hex: String): Hash = Hash(hex)

  /** Implicit conversion from bytes array. */
  implicit def bytesToHash(bytes: Array[Byte]): Hash = Hash(bytes)

  /** Implicit conversion to hexadecimal string. */
  def hashToString(hash: Hash): String = hash.hex

  /** Implicit conversion to bytes array. */
  def hashToBytes(hash: Hash): Array[Byte] = hash.bytes

}

/**
 * Hash value.
 */
class Hash private (val bytes: Array[Byte]) {

  /** Hexadecimal representation. */
  lazy val hex = bytes.map("%02x".format(_)).mkString("")

  override def toString: String = hex

  override def hashCode = hex.hashCode

  override def equals(other: Any) = other match {
    case that: Hash => this.hex == that.hex
    case _ => false
  }

}