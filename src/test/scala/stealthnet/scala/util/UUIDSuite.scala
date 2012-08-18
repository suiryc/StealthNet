package stealthnet.scala.util

import java.util.{UUID => jUUID}
import scala.collection.mutable.WrappedArray

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import stealthnet.scala.cryptography.Hash

// scalastyle:off magic.number
@RunWith(classOf[JUnitRunner])
class UUIDSuite extends FunSuite {

  test("UUID generation") {
    val juuid = jUUID.fromString("01234567-89ab-cdef-fedc-ba9876543210")
    val bytes: Array[Byte] = UUID.javaUuidToBytes(juuid)
    val a = Array[Byte](0x01, 0x23, 0x45, 0x67,
      0x89.asInstanceOf[Byte], 0xab.asInstanceOf[Byte], 0xcd.asInstanceOf[Byte],
      0xef.asInstanceOf[Byte], 0xfe.asInstanceOf[Byte], 0xdc.asInstanceOf[Byte],
      0xba.asInstanceOf[Byte], 0x98.asInstanceOf[Byte], 0x76.asInstanceOf[Byte],
      0x54.asInstanceOf[Byte], 0x32.asInstanceOf[Byte], 0x10.asInstanceOf[Byte])

    assert((a:WrappedArray[Byte]) === (bytes:WrappedArray[Byte]))
  }

}
// scalastyle:on magic.number
