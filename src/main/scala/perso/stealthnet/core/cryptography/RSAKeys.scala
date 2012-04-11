package perso.stealthnet.core.cryptography

import java.security.KeyPairGenerator
import java.security.interfaces.{RSAPublicKey, RSAPrivateKey}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import perso.stealthnet.network.protocol.Constants

object RSAKeys {

  /* XXX - secure enough, or should we specify initialization parameters ?
   * (via JCE / or Bouncy Castle's lightweight API ?)
   * See: http://stackoverflow.com/questions/3087049/bouncy-castle-rsa-keypair-generation-using-lightweight-api
   */
  private val kpGen = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME)
  kpGen.initialize(Constants.RSAKeyLength)
  private val pair = kpGen.generateKeyPair()

  def keys() = pair

  /* XXX - generate a byte array of 129 bytes compared to 128 bytes for stealthnet ? */
  def publicKey() = pair.getPublic.asInstanceOf[RSAPublicKey]

  def privateKey() = pair.getPrivate.asInstanceOf[RSAPrivateKey]

}
