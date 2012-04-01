package perso.stealthnet.core.cryptography

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite

@RunWith(classOf[JUnitRunner])
class HashSuite extends FunSuite {

  private val hex = "0123456789abcdef"
  private val bytes = Array[Byte](0x01 byteValue, 0x23 byteValue, 0x45 byteValue,
      0x67 byteValue, 0x89 byteValue, 0xab byteValue, 0xcd byteValue, 0xef byteValue)

  test("Hexadecimal representation shall match initial one") {
    val a = Hash(hex)
    expect(hex) {
      a.hex
    }
  }

  test("Hexadecimal representation shall match initial bytes array") {
    val a = Hash(bytes)
    expect(hex) {
      a.hex
    }
  }

  test("Bytes array shall match initial one") {
    val a = Hash(bytes)
    expect(bytes) {
      a.bytes
    }
  }

  test("Bytes array shall match initial hexadecimal representation") {
    val a = Hash(hex)
    /* Note: compare as List (content) and not as Array (instance) */
    expect(bytes.toList) {
      a.bytes.toList
    }
  }

  test("Object equality") {
    val a = Hash(hex)
    val b = Hash(bytes)
    assert(a === b)
  }

  test("Implicit conversion from hexadecimal representation") {
    val a = Hash(hex)
    val b: Hash = hex
    assert(a === b)
  }

  test("Implicit conversion from bytes array") {
    val a = Hash(hex)
    val b: Hash = bytes
    assert(a === b)
  }

  test("null bytes array is an illegal argument") {
    intercept[IllegalArgumentException] {
      Hash(null.asInstanceOf[Array[Byte]])
    }
  }

  test("null hexadecimal representation is an illegal argument") {
    intercept[IllegalArgumentException] {
      Hash(null.asInstanceOf[String])
    }
  }

  test("Odd hexadecimal representation") {
    val a = Hash("123")
    val b = Hash(Array[Byte](0x01, 0x23))
    assert(a === b)
  }

}
