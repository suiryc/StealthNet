package stealthnet.scala.cryptography.io

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import stealthnet.scala.cryptography.{Ciphers, RijndaelParameters}

// scalastyle:off magic.number
@RunWith(classOf[JUnitRunner])
class BCCipherInputStreamSuite extends FunSuite {

  private val input = "The quick brown fox jumps over the lazy dog".getBytes("US-ASCII") ++
    (for (i <- 0 to 255) yield i.byteValue)

  def readInputStream(is: InputStream): Array[Byte] = {
    val buffer = new Array[Byte](1024)
    /* Note: foldLeft evaluates Stream elements too early, and we are re-using
     * a buffer; so duplicate it before that
     */
    Stream.continually(is.read(buffer)).
      takeWhile(_ != -1).
      map(buffer.take(_)).
      foldLeft(Array[Byte]())((a, b) => a ++ b)
  }

  test("input stream is decrypted data") {
    val params = RijndaelParameters()
    val encrypter = Ciphers.rijndaelEncrypter(params)
    val decrypter = Ciphers.rijndaelDecrypter(params)
    val baos = new ByteArrayOutputStream()
    val os = new BCCipherOutputStream(baos, encrypter)
    os.write(input)
    os.close()

    val bais = new ByteArrayInputStream(baos.toByteArray())
    val is = new BCCipherInputStream(bais, decrypter)
    val a = readInputStream(is)
    is.close()

    assert(a === input)
  }

  test("read() produces same result than read(bytes)") {
    val params = RijndaelParameters()
    val encrypter = Ciphers.rijndaelEncrypter(params)
    val decrypter = Ciphers.rijndaelDecrypter(params)
    val baos = new ByteArrayOutputStream()
    val os = new BCCipherOutputStream(baos, encrypter)
    os.write(input)
    os.close()

    val bais1 = new ByteArrayInputStream(baos.toByteArray())
    val is1 = new BCCipherInputStream(bais1, decrypter)
    val a = readInputStream(is1)

    val bais2 = new ByteArrayInputStream(baos.toByteArray())
    val is2 = new BCCipherInputStream(bais2, decrypter)
    val b = Stream.continually(is2.read()).
      takeWhile(_ != -1).
      map(_.byteValue).toArray

    assert(b === a)
  }

  test("read() returns value in 0-255 range") {
    val a = input.map(b => (b.intValue + 256) % 256)

    val params = RijndaelParameters()
    val encrypter = Ciphers.rijndaelEncrypter(params)
    val decrypter = Ciphers.rijndaelDecrypter(params)
    val baos = new ByteArrayOutputStream()
    val os = new BCCipherOutputStream(baos, encrypter)
    os.write(input)
    os.close()

    val bais = new ByteArrayInputStream(baos.toByteArray())
    val is = new BCCipherInputStream(bais, decrypter)
    val b = Stream.continually(is.read()).
      takeWhile(_ != -1).toArray

    assert(b === a)
  }

}
// scalastyle:on magic.number
