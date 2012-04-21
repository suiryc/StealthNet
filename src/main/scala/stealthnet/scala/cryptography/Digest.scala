package stealthnet.scala.cryptography

/**
 * Digest algorithms.
 */
object Algorithm extends Enumeration {

  /** Available algorithm: ''SHA-1''. */
  val SHA1 = Value
  /** Available algorithm: ''SHA-256''. */
  val SHA256 = Value
  /** Available algorithm: ''SHA-384''. */
  val SHA384 = Value
  /** Available algorithm: ''SHA-512''. */
  val SHA512 = Value

  private val names: Map[Value, String] = Map(
    SHA1 -> "SHA-1",
    SHA256 -> "SHA-256",
    SHA384 -> "SHA-384",
    SHA512 -> "SHA-512"
  )

  /**
   * Gets the algorithm value by its name.
   *
   * @throws IllegalArgumentException if algorithm is unknown
   */
  def algorithm(name: String): Value =
    names.find(p => p._2 == name) match {
      case Some((value, _)) => value
      case None => throw new IllegalArgumentException("Unhandled algorithm[%s]".format(name))
    }

  /** Gets the algorithm name by its value. */
  def algorithm(value: Value): String = names(value)

}

/**
 * Cryptographic hash function.
 */
trait Digest {

  /** Algorithm name. */
  val algorithm: Algorithm.Value

  /** Updates the digest using the specified array of bytes. */
  def update(bytes: Array[Byte]): Digest

  /** Completes the hash computation. */
  def digest(): Hash

  /** Resets the digest for further use. */
  def reset(): Digest

}

/**
 * Digest class companion object.
 */
object Digest {

  /** Gets a new ''SHA-1'' digest. */
  def sha1: Digest = new MessageDigest(Algorithm.SHA1)

  /** Gets a new ''SHA-256'' digest. */
  def sha256: Digest = new MessageDigest(Algorithm.SHA256)

  /** Gets a new ''SHA-384'' digest. */
  def sha384: Digest = new MessageDigest(Algorithm.SHA384)

  /** Gets a new ''SHA-512'' digest. */
  def sha512: Digest = new MessageDigest(Algorithm.SHA512)

  /** Factory method from algorithm. */
  def apply(algorithm: Algorithm.Value): Digest = algorithm match {
    case Algorithm.SHA1 => sha1
    case Algorithm.SHA256 => sha256
    case Algorithm.SHA384 => sha384
    case Algorithm.SHA512 => sha512
  }

}

/**
 * Message digest derived from `Java`.
 */
private class MessageDigest(val algorithm: Algorithm.Value)
  extends Digest
{

  private val jDigest =
    java.security.MessageDigest.getInstance(Algorithm.algorithm(algorithm))

  def update(bytes: Array[Byte]) = {
    jDigest.update(bytes)
    this
  }

  def digest() = jDigest.digest()

  def reset() = {
    jDigest.reset()
    this
  }

}
