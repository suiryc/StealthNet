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

/**
 * Cipher modes.
 *
 * The different modes and values are the ones available in ''C#'' (on which
 * rely original ''StealthNet'').
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

  /**
   * Gets the id (protocol value) corresponding to a mode.
   *
   * @param v the mode for which to determine the id
   * @return the corresponding protocol value
   */
  def id(v: CipherMode.Value): Byte = v match {
    case CBC => 0x01
    case ECB => 0x02
    case OFB => 0x03
    case CFB => 0x04
    case CTS => 0x05
  }

  /**
   * Gets the mode corresponding to an id.
   *
   * @param v the id for which to determine the mode
   * @return an option value containing the corresponding mode,
   *   or `None` if none exists
   */
  def value(v: Byte): Option[CipherMode.Value] = v match {
    case 0x01 => Some(CBC)
    case 0x02 => Some(ECB)
    case 0x03 => Some(OFB)
    case 0x04 => Some(CFB)
    case 0x05 => Some(CTS)
    case _ => None
  }

}

/**
 * Padding modes.
 *
 * The different modes and values are the ones available in ''C#'' (on which
 * rely original ''StealthNet'').
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

  /**
   * Gets the id (protocol value) corresponding to a mode.
   *
   * @param v the mode for which to determine the id
   * @return the corresponding protocol value
   */
  def id(v: PaddingMode.Value): Byte = v match {
    case None => 0x01
    case PKCS7 => 0x02
    case Zeros => 0x03
    case ANSIX923 => 0x04
    case ISO10126 => 0x05
  }

  /**
   * Gets the mode corresponding to an id.
   *
   * @param v the id for which to determine the mode
   * @return an option value containing the corresponding mode,
   *   or `None` if none exists
   */
  def value(v: Byte): Option[PaddingMode.Value] = v match {
    case 0x01 => Some(None)
    case 0x02 => Some(PKCS7)
    case 0x03 => Some(Zeros)
    case 0x04 => Some(ANSIX923)
    case 0x05 => Some(ISO10126)
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
    KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME).generatePublic(keySpec).asInstanceOf[RSAPublicKey]

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
    val blockCipher: BlockCipher = rijndael.cipherMode match {
      case CipherMode.CBC => new CBCBlockCipher(engine)
      case CipherMode.ECB => engine
      case CipherMode.OFB => new OFBBlockCipher(engine, rijndael.feedbackSize)
      case CipherMode.CFB => new CFBBlockCipher(engine, rijndael.feedbackSize)
      case CipherMode.CTS => return new CTSBlockCipher(engine)
    }
    val padding: BlockCipherPadding = rijndael.paddingMode match {
      case PaddingMode.None => null
      case PaddingMode.PKCS7 => new PKCS7Padding()
      case PaddingMode.Zeros => new ZeroBytePadding()
      case PaddingMode.ANSIX923 => new X923Padding()
      case PaddingMode.ISO10126 => new ISO10126d2Padding()
    }

    val cipher = if (padding != null)
        new PaddedBufferedBlockCipher(blockCipher, padding)
      else
        new BufferedBlockCipher(blockCipher)
    cipher.init(encryption, new ParametersWithIV(new KeyParameter(rijndael.key), rijndael.iv))

    cipher
  }

  /**
   * Gets new ''Rijndael'' cipher to encrypt.
   *
   * @param rijndael ''Rijndael'' parameters
   * @return ''Rijndael'' cipher using parameters to encrypt
   */
  def rijndaelEncrypter(rijndael: RijndaelParameters) =
    rijndaelCipher(rijndael, true)

  /**
   * Gets new ''Rijndael'' cipher to decrypt.
   *
   * @param rijndael ''Rijndael'' parameters
   * @return ''Rijndael'' cipher using parameters to decrypt
   */
  def rijndaelDecrypter(rijndael: RijndaelParameters) =
    rijndaelCipher(rijndael, false)

}
