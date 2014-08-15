package stealthnet.scala.cryptography

import java.security.{Key, KeyFactory, PrivateKey, PublicKey}
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import javax.crypto.Cipher
import org.bouncycastle.crypto.{BlockCipher, BufferedBlockCipher}
import org.bouncycastle.crypto.engines.RijndaelEngine
import org.bouncycastle.crypto.params.{KeyParameter, ParametersWithIV}
import org.bouncycastle.crypto.paddings.{
  BlockCipherPadding,
  ISO10126d2Padding,
  PaddedBufferedBlockCipher,
  PKCS7Padding,
  X923Padding,
  ZeroBytePadding
}
import org.bouncycastle.crypto.modes.{
  CFBBlockCipher,
  CTSBlockCipher,
  CBCBlockCipher,
  OFBBlockCipher
}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import scala.language.implicitConversions

/**
 * Cipher modes.
 *
 * The different modes and values are the ones available in ''C#'' (on which
 * rely original ''StealthNet'' application):
 * {{{
 * public enum CipherMode { CBC = 1, ECB = 2, OFB = 3, CFB = 4, CTS = 5 }
 * }}}
 */
object CipherMode extends Enumeration {

  /** Available mode: '''C'''ipher '''B'''lock '''C'''haining. */
  val CBC = Value
  /** Available mode: '''E'''lectronic '''C'''ode'''B'''ook. */
  val ECB = Value
  /** Available mode: '''O'''utput '''F'''eed'''B'''ack. */
  val OFB = Value
  /** Available mode: '''C'''ipher '''F'''eed'''B'''ack. */
  val CFB = Value
  /** Available mode: '''C'''ipher '''T'''ext '''S'''tealing. */
  val CTS = Value

  private val CBC_id: Byte = 0x01
  private val ECB_id: Byte = 0x02
  private val OFB_id: Byte = 0x03
  private val CFB_id: Byte = 0x04
  private val CTS_id: Byte = 0x05

  /**
   * Gets the id (protocol value) corresponding to a mode.
   *
   * @param v the mode for which to determine the id
   * @return the corresponding protocol value
   */
  def id(v: CipherMode.Value): Byte = v match {
    case CBC => CBC_id
    case ECB => ECB_id
    case OFB => OFB_id
    case CFB => CFB_id
    case CTS => CTS_id
  }

  /**
   * Gets the mode corresponding to an id.
   *
   * @param v the id for which to determine the mode
   * @return an option value containing the corresponding mode,
   *   or `None` if none exists
   */
  def value(v: Byte): Option[CipherMode.Value] = v match {
    case CBC_id => Some(CBC)
    case ECB_id => Some(ECB)
    case OFB_id => Some(OFB)
    case CFB_id => Some(CFB)
    case CTS_id => Some(CTS)
    case _ => None
  }

}

/**
 * Padding modes.
 *
 * The different modes and values are the ones available in ''C#'' (on which
 * rely original ''StealthNet'' application):
 * {{{
 * public enum PaddingMode { None = 1, PKCS7 = 2, Zeros = 3, ANSIX923 = 4, ISO10126 = 5 }
 * }}}
 */
object PaddingMode extends Enumeration {

  /** Available mode: no padding. */
  val None = Value
  /** Available mode: ''PKCS #7''. */
  val PKCS7 = Value
  /** Available mode: zero bytes. */
  val Zeros = Value
  /** Available mode: ''ANSI X.923''. */
  val ANSIX923 = Value
  /** Available mode: ''ISO 10126''. */
  val ISO10126 = Value

  private val None_id: Byte = 0x01
  private val PKCS7_id: Byte = 0x02
  private val Zeros_id: Byte = 0x03
  private val ANSIX923_id: Byte = 0x04
  private val ISO10126_id: Byte = 0x05

  /**
   * Gets the id (protocol value) corresponding to a mode.
   *
   * @param v the mode for which to determine the id
   * @return the corresponding protocol value
   */
  def id(v: PaddingMode.Value): Byte = v match {
    case None => None_id
    case PKCS7 => PKCS7_id
    case Zeros => Zeros_id
    case ANSIX923 => ANSIX923_id
    case ISO10126 => ISO10126_id
  }

  /**
   * Gets the mode corresponding to an id.
   *
   * @param v the id for which to determine the mode
   * @return an option value containing the corresponding mode,
   *   or `None` if none exists
   */
  def value(v: Byte): Option[PaddingMode.Value] = v match {
    case None_id => Some(None)
    case PKCS7_id => Some(PKCS7)
    case Zeros_id => Some(Zeros)
    case ANSIX923_id => Some(ANSIX923)
    case ISO10126_id => Some(ISO10126)
    case _ => scala.None
  }

}

/**
 * Cipher helper.
 *
 * Provides necessary ciphers (''RSA'' and ''Rijndael'') to encrypt/decrypt
 * data.
 *
 * @todo should we call BouncyCastle directly for RSA, instead of using JCE ?
 */
object Ciphers {

  /**
   * Implicit conversion from ''RSA'' public key specification (modulus and
   * exponent) to proper ''RSA'' public key.
   */
  implicit def keySpecToKey(keySpec: RSAPublicKeySpec): RSAPublicKey =
    KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME)
      .generatePublic(keySpec).asInstanceOf[RSAPublicKey]

  /**
   * Gets new ''RSA'' cipher.
   *
   * @param key ''RSA'' key: public one for encrypting,
   *   private one for decrypting
   * @param mode cipher mode: `Cipher.ENCRYPT_MODE` or `Cipher.DECRYPT_MODE`
   */
  private def rsaCipher(key: Key, mode: Int): Cipher = {
    val cipher = Cipher.getInstance("RSA")
    cipher.init(mode, key)
    cipher
  }

  /**
   * Gets new ''RSA'' cipher to encrypt.
   *
   * @param key ''RSA'' public key
   * @return ''RSA'' cipher using key to encrypt
   */
  def rsaEncrypter(key: PublicKey) = rsaCipher(key, Cipher.ENCRYPT_MODE)

  /**
   * Gets new ''RSA'' cipher to decrypt.
   *
   * @param key ''RSA'' private key
   * @return ''RSA'' cipher using key to decrypt
   */
  def rsaDecrypter(key: PrivateKey) = rsaCipher(key, Cipher.DECRYPT_MODE)

  /**
   * Gets new ''Rijndael'' cipher.
   *
   * @param rijndael ''Rijndael'' parameters
   * @param encryption whether the cipher is initialized for encryption (`true`)
   *   or decryption (`false`)
   */
  private def rijndaelCipher(rijndael: RijndaelParameters, encryption: Boolean): BufferedBlockCipher =
  {
    val engine = new RijndaelEngine(rijndael.blockSize)

    /* Note: ECB and CTS don't use IV, and only expect the key as parameter */
    val (blockCipher, useIV): (Either[BlockCipher, BufferedBlockCipher], Boolean) =
      rijndael.cipherMode match {
        case CipherMode.CBC => (Left(new CBCBlockCipher(engine)), true)
        case CipherMode.ECB => (Left(engine), false)
        case CipherMode.OFB => (Left(new OFBBlockCipher(engine, rijndael.feedbackSize)), true)
        case CipherMode.CFB => (Left(new CFBBlockCipher(engine, rijndael.feedbackSize)), true)
        case CipherMode.CTS => (Right(new CTSBlockCipher(engine)), false)
      }
    /* Note: CTS does not use padding at all */
    val cipher = blockCipher.fold({ blockCipher =>
      val padding: Option[BlockCipherPadding] = rijndael.paddingMode match {
        case PaddingMode.None => None
        case PaddingMode.PKCS7 => Some(new PKCS7Padding())
        case PaddingMode.Zeros => Some(new ZeroBytePadding())
        case PaddingMode.ANSIX923 => Some(new X923Padding())
        case PaddingMode.ISO10126 => Some(new ISO10126d2Padding())
      }

      padding map {
        new PaddedBufferedBlockCipher(blockCipher, _)
      }  getOrElse {
        new BufferedBlockCipher(blockCipher)
      }
    }, { blockCipher => blockCipher })

    cipher.init(encryption,
      if (!useIV) new KeyParameter(rijndael.key)
      else new ParametersWithIV(new KeyParameter(rijndael.key), rijndael.iv)
    )

    cipher
  }

  /**
   * Gets new ''Rijndael'' cipher to encrypt.
   *
   * @param rijndael ''Rijndael'' parameters
   * @return ''Rijndael'' cipher using parameters to encrypt
   */
  def rijndaelEncrypter(rijndael: RijndaelParameters) =
    rijndaelCipher(rijndael, encryption = true)

  /**
   * Gets new ''Rijndael'' cipher to decrypt.
   *
   * @param rijndael ''Rijndael'' parameters
   * @return ''Rijndael'' cipher using parameters to decrypt
   */
  def rijndaelDecrypter(rijndael: RijndaelParameters) =
    rijndaelCipher(rijndael, encryption = false)

}
