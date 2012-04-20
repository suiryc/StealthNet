package perso.stealthnet.cryptography

import java.security.KeyPairGenerator
import java.security.interfaces.{RSAPublicKey, RSAPrivateKey}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import perso.stealthnet.network.protocol.Constants

/**
 * RSA public/private keys.
 * <p>
 * A key pair is generated once when needed.
 *
 * XXX - do default JCE KeyPairGenerator generate secure enough keys, or should
 * we specify initialization parameters (via JCE or directly use BouncyCastle) ?
 * See: http://stackoverflow.com/questions/3087049/bouncy-castle-rsa-keypair-generation-using-lightweight-api
 */
object RSAKeys {

  private val kpGen = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME)
  kpGen.initialize(Constants.RSAKeyLength)
  private val pair = kpGen.generateKeyPair()

  /** Gets RSA key pair. */
  def keys() = pair

  /** Gets RSA public key. */
  def publicKey() = pair.getPublic.asInstanceOf[RSAPublicKey]

  /** Gets RSA private key. */
  def privateKey() = pair.getPrivate.asInstanceOf[RSAPrivateKey]

}
