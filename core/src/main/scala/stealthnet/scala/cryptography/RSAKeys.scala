package stealthnet.scala.cryptography

import java.security.{KeyPair, KeyPairGenerator}
import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import stealthnet.scala.Constants

/**
 * ''RSA'' public/private keys.
 *
 * A key pair is generated once when needed. ''BouncyCastle'' is used as
 * provider.
 *
 * @todo do default JCE KeyPairGenerator generate secure enough keys, or should
 *   we specify initialization parameters (via JCE or directly use BouncyCastle) ?
 *   See: [[http://stackoverflow.com/questions/3087049/bouncy-castle-rsa-keypair-generation-using-lightweight-api]]
 */
object RSAKeys {

  private val kpGen = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME)
  kpGen.initialize(Constants.RSAKeyLength)
  private val pair = kpGen.generateKeyPair()

  /** Gets ''RSA'' key pair. */
  def keys: KeyPair = pair

  /** Gets ''RSA'' public key. */
  def publicKey: RSAPublicKey = pair.getPublic.asInstanceOf[RSAPublicKey]

  /** Gets ''RSA'' private key. */
  def privateKey: RSAPrivateKey = pair.getPrivate.asInstanceOf[RSAPrivateKey]

}
