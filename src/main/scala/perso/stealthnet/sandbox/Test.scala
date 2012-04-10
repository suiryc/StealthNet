package perso.stealthnet.sandbox

import java.security.Security
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.bouncycastle.jce.provider.BouncyCastleProvider
import perso.stealthnet.core.cryptography.Hash
import perso.stealthnet.network.StealthNetClient
import perso.stealthnet.network.StealthNetServer
import perso.stealthnet.webservices.UpdateClient
import perso.stealthnet.webservices.WebCacheClient
import perso.stealthnet.network.protocol.SearchCommand
import perso.stealthnet.network.StealthNetConnection
import perso.stealthnet.network.protocol.Constants

object Test {
  def main(args: Array[String]): Unit = {
    println("Started")

    /* XXX - JCE Unlimited Strength Jurisdiction Policy Files are needed for keys > 128-bits */
    Security.addProvider(new BouncyCastleProvider())

    /* Technically, it is 'Rijndael' (a superset) because 'AES' shall be limited
     * to 128-bits block size.
     */
    val scheme = "Rijndael"
    //val keygen = KeyGenerator.getInstance("Rijndael");
    //keygen.init(256);
    //val cipher = Cipher.getInstance("Rijndael/CBC/PKCS7Padding")

    val input = "test".getBytes()
    //val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
    //val spec = new PBEKeySpec("abcdef".toCharArray(), "ghij".getBytes(), 65536, 256);
    //val tmpKey = factory.generateSecret(spec);
    //val key = /*Array[Byte]()*/tmpKey.getEncoded()
    val key: Array[Byte] = Hash.hashToBytes(Hash("2957f6d6d9c71d97d31af3d3b765c92d4a8ca665142c9b90364496c1ab7be2f3"))
    println(Hash.bytesToHash(key))
    val keySpec = new SecretKeySpec(key, scheme);
    val IV: Array[Byte] = Hash.hashToBytes(Hash("976697d73a33759dac654cb06461bb23"))
    val ivSpec = new IvParameterSpec(IV);
    val cipher = Cipher.getInstance(scheme + "/CBC/PKCS7Padding", BouncyCastleProvider.PROVIDER_NAME);
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
    val output = cipher.doFinal(input)
    println(Hash.bytesToHash(output))

    val decipher = Cipher.getInstance(scheme + "/CBC/PKCS7Padding", BouncyCastleProvider.PROVIDER_NAME);
    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
    val decrypted = cipher.doFinal(output)
    println(new String(decrypted))

    /*
    UpdateClient.getWebCaches("http://rshare.de/rshareupdates.asmx") match {
      case Some(webCaches) =>
        for (webCache <- webCaches) {
          println("WebCache: " + webCache)
          WebCacheClient.getPeer(webCache) match {
            case Some(peer) => println("Got peer: " + peer)
            case None =>
          }
        }

      case None =>
    }
    */

    var client1: StealthNetClient = null
    try {
      StealthNetServer.start
      client1 = new StealthNetClient("127.0.0.1", 8080)
      client1.start()

      Thread.sleep(5000)
    }
    finally {
      if (client1 != null) {
        client1.stop()
      }
      StealthNetServer.stop()
    }
  }

}
