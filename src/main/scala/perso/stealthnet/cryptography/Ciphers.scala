package perso.stealthnet.cryptography

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

object CipherMode extends Enumeration {

  /** Available modes */
  val CBC, ECB, OFB, CFB, CTS, Unknown = Value

  def id(v: CipherMode.Value): Byte = v match {
    case CBC => 0x01
    case ECB => 0x02
    case OFB => 0x03
    case CFB => 0x04
    case CTS => 0x05
    case _ => 0xFF.asInstanceOf[Byte]
  }

  def value(v: Byte): CipherMode.Value = v match {
    case 0x01 => CBC
    case 0x02 => ECB
    case 0x03 => OFB
    case 0x04 => CFB
    case 0x05 => CTS
    case _ => Unknown
  }

}

object PaddingMode extends Enumeration {

  val None, PKCS7, Zeros, ANSIX923, ISO10126, Unknown = Value

  def id(v: PaddingMode.Value): Byte = v match {
    case None => 0x01
    case PKCS7 => 0x02
    case Zeros => 0x03
    case ANSIX923 => 0x04
    case ISO10126 => 0x05
    case _ => 0xFF.asInstanceOf[Byte]
  }

  def value(v: Byte): PaddingMode.Value = v match {
    case 0x01 => None
    case 0x02 => PKCS7
    case 0x03 => Zeros
    case 0x04 => ANSIX923
    case 0x05 => ISO10126
    case _ => Unknown
  }

}

object Ciphers {

  /* XXX - call BouncyCastle directly for RSA ? */

  implicit def keySpecToKey(keySpec: RSAPublicKeySpec): RSAPublicKey =
    KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME).generatePublic(keySpec).asInstanceOf[RSAPublicKey]

  private def rsaCipher(key: Key, mode: Int): Cipher = {
    val cipher = Cipher.getInstance("RSA")
    cipher.init(mode, key)
    cipher
  }

  def rsaEncrypter(key: PublicKey) = rsaCipher(key, Cipher.ENCRYPT_MODE)

  def rsaDecrypter(key: PrivateKey) = rsaCipher(key, Cipher.DECRYPT_MODE)

  private def rijndaelCipher(rijndael: RijndaelParameters, encryption: Boolean): BufferedBlockCipher = {
    val engine = new RijndaelEngine(rijndael.blockSize)
    val blockCipher: BlockCipher = rijndael.cipherMode match {
      case CipherMode.CBC => new CBCBlockCipher(engine)
      case CipherMode.ECB => engine
      case CipherMode.OFB => new OFBBlockCipher(engine, rijndael.feedbackSize)
      case CipherMode.CFB => new CFBBlockCipher(engine, rijndael.feedbackSize)
      case CipherMode.CTS => return new CTSBlockCipher(engine)
      /* XXX - cleanly handle issue */
      case _ => throw new Exception()
    }
    val padding: BlockCipherPadding = rijndael.paddingMode match {
      case PaddingMode.None => null
      case PaddingMode.PKCS7 => new PKCS7Padding()
      case PaddingMode.Zeros => new ZeroBytePadding()
      case PaddingMode.ANSIX923 => new X923Padding()
      case PaddingMode.ISO10126 => new ISO10126d2Padding()
      /* XXX - cleanly handle issue */
      case _ => throw new Exception()
    }

    val cipher = if (padding != null)
        new PaddedBufferedBlockCipher(blockCipher, padding)
      else
        new BufferedBlockCipher(blockCipher)
    cipher.init(encryption, new ParametersWithIV(new KeyParameter(rijndael.key), rijndael.iv))

    cipher
  }

  def rijndaelEncrypter(rijndael: RijndaelParameters) = rijndaelCipher(rijndael, true)

  def rijndaelDecrypter(rijndael: RijndaelParameters) = rijndaelCipher(rijndael, false)

}
