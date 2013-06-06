package stealthnet.scala.cryptography.io

import java.io.ByteArrayOutputStream

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import stealthnet.scala.cryptography.{Ciphers, RijndaelParameters}

@RunWith(classOf[JUnitRunner])
class BCCipherOutputStreamSuite extends FunSuite {

  private val input = "The quick brown fox jumps over the lazy dog".getBytes("US-ASCII") ++
    (for (i <- 0 to 255) yield i.byteValue)

  test("output stream is encrypted data") {
    val params = RijndaelParameters()
    val encrypter = Ciphers.rijndaelEncrypter(params)
    val baos = new ByteArrayOutputStream()
    val os = new BCCipherOutputStream(baos, encrypter)

    var inputLength = input.length
    var encryptedLength = encrypter.getOutputSize(inputLength)
    val encrypted = new Array[Byte](encryptedLength)
    encryptedLength = encrypter.processBytes(input, 0, inputLength, encrypted, 0)
    encryptedLength += encrypter.doFinal(encrypted, encryptedLength)

    encrypter.reset()
    os.write(input)
    os.close()
    val a = baos.toByteArray()
    assert(a === encrypted)
  }

  test("write(byte) produces same result than write(bytes)") {
    val params = RijndaelParameters()
    val encrypter = Ciphers.rijndaelEncrypter(params)
    val baos1 = new ByteArrayOutputStream()
    val os1 = new BCCipherOutputStream(baos1, encrypter)

    encrypter.reset()
    os1.write(input)
    os1.close()
    val a = baos1.toByteArray()

    val baos2 = new ByteArrayOutputStream()
    val os2 = new BCCipherOutputStream(baos2, encrypter)

    encrypter.reset()
    input.foreach(os2.write(_))
    os2.close()
    val b = baos2.toByteArray()

    assert(a === b)
  }

}
