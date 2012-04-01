package perso.stealthnet.core.cryptography

/**
 * Algorithms.
 */
object Algorithm extends Enumeration {

  /** Available algorithms. */
  val SHA1, SHA256, SHA384, SHA512 = Value

  private val names: Map[Value, String] = Map(
      SHA1 -> "SHA-1",
      SHA256 -> "SHA-256",
      SHA384 -> "SHA-384",
      SHA512 -> "SHA-512"
  )

  /** Get algorithm value from name. */
  def algorithm(name: String): Value =
    names.find(p => p._2 == name) match {
      case Some((value, _)) => value
      case None => throw new IllegalArgumentException("Unhandled algorithm[%s]".format(name))
    }

  /** Get algorithm name from value. */
  def algorithm(value: Value): String =
    names.get(value) match {
      case Some(name) => name
      case None => throw new IllegalArgumentException("Unhandled algorithm[%s]".format(value))
    }

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

  def sha1: Digest = new MessageDigest(Algorithm.SHA1)

  def sha256: Digest = new MessageDigest(Algorithm.SHA256)

  def sha384: Digest = new MessageDigest(Algorithm.SHA384)

  def sha512: Digest = new MessageDigest(Algorithm.SHA512)

  def apply(algorithm: Algorithm.Value): Digest = algorithm match {
    case Algorithm.SHA1 => sha1
    case Algorithm.SHA256 => sha256
    case Algorithm.SHA384 => sha384
    case Algorithm.SHA512 => sha512
  }

}

/**
 * Message digest derived from Java.
 */
private class MessageDigest(val algorithm: Algorithm.Value)
    extends Digest
{

  private val jDigest = java.security.MessageDigest.getInstance(Algorithm.algorithm(algorithm))

  def update(bytes: Array[Byte]): Digest = {
    jDigest.update(bytes)
    this
  }

  def digest(): Hash = jDigest.digest()

  def reset(): Digest = {
    jDigest.reset()
    this
  }

}
