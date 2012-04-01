package perso.stealthnet.core.cryptography

/**
 * Message class companion object.
 */
object Message {

  /** Factory method from bytes array. */
  def apply(bytes: Array[Byte]) = new Message(bytes)

  /** Factory method from message string. */
  def apply(message: String) = new Message(message)

  /** Hash message. */
  def hash(bytes: Array[Byte], algorithm: Algorithm.Value): Hash =
    Message(bytes).hash(algorithm)

  /** Hash message. */
  def hash(message: String, algorithm: Algorithm.Value): Hash =
    Message(message).hash(algorithm)

}

/**
 * Message to hash.
 */
class Message(val bytes: Array[Byte]) {

  /** Hash cache. */
  private var cache = Map[Algorithm.Value, Hash]()

  /**
   * Ctor from message string.
   * Message is converted to bytes according to default charset.
   */
  def this(message: String) = this(message.getBytes())

  /**
   * Hash message for given algorithm.
   * Result is cached.
   */
  def hash(algorithm: Algorithm.Value): Hash = {
    cache.get(algorithm) match {
      case Some(hash) => hash
      case None => {
        val digest = Digest(algorithm)
        digest.update(bytes)
        val result = digest.digest()
        cache += (algorithm -> result)
        result
      }
    }
  }

}
