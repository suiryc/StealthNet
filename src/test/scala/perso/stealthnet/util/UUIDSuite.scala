package perso.stealthnet.util

import java.util.{UUID => jUUID}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import perso.stealthnet.core.cryptography.Hash

@RunWith(classOf[JUnitRunner])
class UUIDSuite extends FunSuite {

  test("UUID generation") {
    val juuid = jUUID.fromString("01234567-89ab-cdef-fedc-ba9876543210")
    val bytes: Array[Byte] = UUID.javaUuidToBytes(juuid)
    val a = Array[Byte](0x01 byteValue, 0x23 byteValue, 0x45 byteValue, 0x67 byteValue,
        0x89 byteValue, 0xab byteValue, 0xcd byteValue, 0xef byteValue,
        0xfe byteValue, 0xdc byteValue, 0xba byteValue, 0x98 byteValue,
        0x76 byteValue, 0x54 byteValue, 0x32 byteValue, 0x10 byteValue)

    expect(a.toList) {
      bytes.toList
    }
  }

}