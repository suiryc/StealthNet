package stealthnet.scala.network.connection

import io.netty.channel.Channel
import java.security.interfaces.RSAPublicKey
import java.util.Date
import org.bouncycastle.crypto.BufferedBlockCipher
import stealthnet.scala.cryptography.{Ciphers, RijndaelParameters}
import stealthnet.scala.util.Peer
import stealthnet.scala.util.log.LoggingContext

/**  ''StealthNet'' connection companion object. */
object StealthNetConnection {

  def loggerContext(cnx: Option[StealthNetConnection], channel: Channel) = {
    val ctx = cnx map(_.loggerContext) getOrElse(Nil)

    if (ctx.find(_._1 == "peer").isDefined)
      ctx
    else
      ctx ::: List("remote" -> channel.remoteAddress)
  }

}

/**
 * ''StealthNet'' connection associated to a channel.
 *
 * @note When setting the local or remote ''Rijndael'' parameters, the
 *   corresponding encrypter/decrypter is created as a side effect. It is to be
 *   reseted and used when necessary, instead of creating a new one each time.
 *   This is not done for ''RSA'' which is used only once during handshaking.
 */
class StealthNetConnection protected[connection] (val channel: Channel)
  extends StealthNetConnectionParameters()
  with LoggingContext
{

  // scalastyle:off null
  assert(channel != null)
  // scalastyle:on null

  val createDate = new Date()

  def loggerContext: List[(String, Any)] =
    peer map(v => List("peer" -> v)) getOrElse(Nil)

  /** Remote peer. */
  var peer: Option[Peer] = None
  /** Whether connection was accepted (no limit reached). */
  var accepted: Boolean = true
  /** Whether connection was established (handshake successful). */
  var established: Boolean = false
  /** Whether connection is being closed. */
  var closing: Boolean = false
  /** Remote ''RSA'' public key to encrypt data. */
  var remoteRSAKey: Option[RSAPublicKey] = None

  /** Local ''Rijndael'' parameters to encrypt data. */
  private[this] var localRijndaelParams: Option[RijndaelParameters] = None
  /** ''Rijndael'' encrypter. */
  var rijndaelEncrypter: Option[BufferedBlockCipher] = None

  /** Gets local ''Rijndael'' parameters to encrypt data. */
  def localRijndaelParameters: Option[RijndaelParameters] = localRijndaelParams

  /**
   * Sets local ''Rijndael'' parameters to encrypt data.
   *
   * As a side effect, also creates the related encrypter.
   */
  def localRijndaelParameters_=(params: RijndaelParameters) {
    localRijndaelParams = Some(params)
    rijndaelEncrypter = Some(Ciphers.rijndaelEncrypter(params))
  }

  /** Remote ''Rijndael'' parameters to decrypt data. */
  private[this] var remoteRijndaelParams: Option[RijndaelParameters] = None
  /** ''Rijndael'' decrypter. */
  var rijndaelDecrypter: Option[BufferedBlockCipher] = None

  /** Gets remote ''Rijndael'' parameters to decrypt data. */
  def remoteRijndaelParameters: Option[RijndaelParameters] = remoteRijndaelParams

  /**
   * Sets remote ''Rijndael'' parameters to decrypt data.
   *
   * As a side effect, also creates the related decrypter.
   */
  def remoteRijndaelParameters_=(params: RijndaelParameters) {
    remoteRijndaelParams = Some(params)
    rijndaelDecrypter = Some(Ciphers.rijndaelDecrypter(params))
  }

  /** Received commands. */
  var receivedCommands: Int = _
  /** Sent commands. */
  var sentCommands: Int = _

  /** Closes the connection channel. */
  def close() {
    closing = true
    channel.close()
  }

}
