package perso.stealthnet.util

import java.util.{UUID => jUUID}

import scala.collection.mutable.ListBuffer

import perso.stealthnet.core.cryptography.Hash

/**
 * UUID class companion object
 */
object UUID {

  /** Factory method from bytes array. */
  def apply(bytes: Array[Byte]): UUID = {
    require(bytes != null)

    new UUID(bytes)
  }

  /** Implicit conversion from Java UUID to bytes array. */
  implicit def javaUuidToBytes(uuid: jUUID): Array[Byte] = {
    val result: ListBuffer[Byte] = ListBuffer.empty

    for (value <- List(uuid.getLeastSignificantBits(), uuid.getMostSignificantBits())) {
      for (idx <- 0 to 7) {
        ((value >>> (idx * 8)) & 0xFF).byteValue() +=: result
      }
    }

    result.toArray
  }

  /**Implicit conversion to Hash. */
  implicit def uuidToHash(uuid: UUID): Hash = Hash(uuid.bytes)

  /** Generates a random UUID. */
  def generate(): UUID = new UUID(jUUID.randomUUID())

}

/**
 * UUID
 */
class UUID private (val bytes: Array[Byte]) {

  /* XXX - toString ? */

}
