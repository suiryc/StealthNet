package stealthnet.scala.util

import java.util.{UUID => jUUID}

import scala.collection.mutable.ListBuffer

import stealthnet.scala.cryptography.Hash

/**
 * UUID class companion object
 */
object UUID {

  /** Factory method from bytes array. */
  def apply(bytes: Array[Byte]): UUID = {
    // scalastyle:off null
    require(bytes != null)
    // scalastyle:on null

    new UUID(bytes)
  }

  /** Implicit conversion from Java UUID to bytes array. */
  implicit def javaUuidToBytes(uuid: jUUID): Array[Byte] = {
    val result: ListBuffer[Byte] = ListBuffer.empty

    for (value <- List(uuid.getLeastSignificantBits(), uuid.getMostSignificantBits())) {
      for (idx <- 0 to 7) {
        ((value >>> (idx * 8)) & 0xFF).asInstanceOf[Byte] +=: result
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
 *
 * @todo Override toString ?
 */
class UUID private (val bytes: Array[Byte])
