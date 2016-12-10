package stealthnet.scala.cryptography

import java.security.{SecureRandom, Security}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import scala.collection.mutable
import stealthnet.scala.Constants

// scalastyle:off magic.number
@RunWith(classOf[JUnitRunner])
class CiphersSuite extends FunSuite {

  /* Register BouncyCastle if necessary */
  if (Option(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)).isEmpty)
    Security.addProvider(new BouncyCastleProvider())

  private val blockSizes = List(128, 160, 192, 224, 256)
  private val keyLengths = List(128, 160, 192, 224, 256)
  private val feedbackSizes = List(64, 128, 256)
  private val random = new SecureRandom()
  private val input = "The quick brown fox jumps over the lazy dog".getBytes("US-ASCII")

  test("Rijndael encryption/decryption") {
    /* Notes:
     *  - feedback size must no exceed block size
     *  - some tests are actually redundant since some cipher modes do not use
     *    padding or feedback
     */
    for {
      blockSize <- blockSizes
      feedbackSize <- feedbackSizes if feedbackSize <= blockSize
      keySize <- keyLengths
      cipherMode <- CipherMode.values
      paddingMode <- PaddingMode.values
    } {
      val iv = new Array[Byte](blockSize / 8)
      val key = new Array[Byte](keySize / 8)
      random.nextBytes(iv)
      random.nextBytes(key)

      val params = new RijndaelParameters(blockSize, feedbackSize, keySize, cipherMode,
        paddingMode, iv, key)
      val encrypter = try {
        Ciphers.rijndaelEncrypter(params)
      } catch {
        case e: Throwable =>
          throw new Error(
            s"Failed to create Rijndael blockSize[$blockSize] feedbackSize[$feedbackSize]" +
            s" keySize[$keySize] cipherMode[$cipherMode] paddingMode[$paddingMode] encrypter",
            e
          )
      }
      val decrypter = try {
        Ciphers.rijndaelDecrypter(params)
      } catch {
        case e: Throwable =>
          throw new Error(
            s"Failed to create Rijndael blockSize[$blockSize] feedbackSize[$feedbackSize]" +
            s" keySize[$keySize] cipherMode[$cipherMode] paddingMode[$paddingMode] decrypter",
            e
          )
      }

      /* test encryption */
      val actualInput = paddingMode match {
        case PaddingMode.None =>
          val need = (blockSize / 8) - input.length % (blockSize / 8)
          val padding = for (_ <- 1 to need) yield 0xFF.byteValue
          input ++ padding

        case _ =>
          input
      }
      var inputLength = actualInput.length
      var encryptedLength = encrypter.getOutputSize(inputLength)
      val encrypted = new Array[Byte](encryptedLength)
      try {
        encryptedLength = encrypter.processBytes(actualInput, 0, inputLength, encrypted, 0)
        encryptedLength += encrypter.doFinal(encrypted, encryptedLength)
      } catch {
        case e: Throwable =>
          throw new Error(
            s"Failed to process data with Rijndael blockSize[$blockSize] feedbackSize[$feedbackSize]" +
            s" keySize[$keySize] cipherMode[$cipherMode] paddingMode[$paddingMode] encrypter",
            e
          )
      }

      /* Note: for Array, '===' compares the content, while '=='/'!=' compares object reference */
      assert((encrypted:mutable.WrappedArray[Byte]) != (input:mutable.WrappedArray[Byte]),
        s"Rijndael blockSize[$blockSize] feedbackSize[$feedbackSize] keySize[$keySize]" +
        s" cipherMode[$cipherMode] paddingMode[$paddingMode] encrypted data equals unencrypted data"
      )

      /* test decryption */
      inputLength = encryptedLength
      var decryptedLength = decrypter.getOutputSize(inputLength)
      val decrypted = new Array[Byte](decryptedLength)
      try {
        decryptedLength = decrypter.processBytes(encrypted, 0, inputLength, decrypted, 0)
        decryptedLength += decrypter.doFinal(decrypted, decryptedLength)
      } catch {
        case e: Throwable =>
          throw new Error(
            s"Failed to process data with Rijndael blockSize[$blockSize] feedbackSize[$feedbackSize]" +
            s" keySize[$keySize] cipherMode[$cipherMode] paddingMode[$paddingMode] decrypter",
            e
          )
      }

      assert(decrypted.take(decryptedLength) === actualInput,
        s"Rijndael blockSize[$blockSize] feedbackSize[$feedbackSize] keySize[$keySize]" +
        s" cipherMode[$cipherMode] paddingMode[$paddingMode] decrypted data does not equal initial data"
      )
    }
  }

  test("RSA encryption/decryption") {
      val encrypter = Ciphers.rsaEncrypter(RSAKeys.publicKey)
      val decrypter = Ciphers.rsaDecrypter(RSAKeys.privateKey)

      /* test encryption */
      var inputLength = input.length
      var encryptedLength = encrypter.getOutputSize(inputLength)
      val encrypted = new Array[Byte](encryptedLength)
      try {
        encryptedLength = encrypter.update(input, 0, inputLength, encrypted, 0)
        encryptedLength += encrypter.doFinal(encrypted, encryptedLength)
      } catch {
        case e: Throwable =>
          throw new Error(
            s"Failed to process data with RSA keySize[${Constants.RSAKeyLength}] encrypter",
            e
          )
      }

      assert((encrypted:mutable.WrappedArray[Byte]) != (input:mutable.WrappedArray[Byte]),
        s"RSA keySize[${Constants.RSAKeyLength}] encrypted data equals unencrypted data"
      )

      /* test decryption */
      inputLength = encryptedLength
      var decryptedLength = decrypter.getOutputSize(inputLength)
      val decrypted = new Array[Byte](decryptedLength)
      try {
        decryptedLength = decrypter.update(encrypted, 0, inputLength, decrypted, 0)
        decryptedLength += decrypter.doFinal(decrypted, decryptedLength)
      } catch {
        case e: Throwable =>
          throw new Error(
            s"Failed to process data with RSA keySize[${Constants.RSAKeyLength}] decrypter",
            e
          )
      }

      assert(decrypted.take(decryptedLength) === input,
        s"RSA keySize[${Constants.RSAKeyLength}] decrypted data does not equal initial data"
      )
  }

}
// scalastyle:on magic.number
