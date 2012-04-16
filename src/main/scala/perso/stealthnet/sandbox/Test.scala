package perso.stealthnet.sandbox

import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import perso.stealthnet.core.cryptography.Ciphers
import perso.stealthnet.core.cryptography.RijndaelParameters
import perso.stealthnet.core.cryptography.io.BCCipherOutputStream
import java.io.ByteArrayOutputStream
import perso.stealthnet.core.cryptography.io.BCCipherInputStream
import java.io.ByteArrayInputStream
import perso.stealthnet.core.cryptography.RSAKeys
import javax.crypto.CipherOutputStream
import javax.crypto.CipherInputStream
import java.security.spec.RSAPublicKeySpec
import perso.stealthnet.core.util.HexDumper
import java.security.KeyFactory
import java.security.spec.RSAPrivateKeySpec
import java.math.BigInteger
import perso.stealthnet.core.cryptography.Hash
import java.security.interfaces.RSAPrivateKey
import perso.stealthnet.network.protocol.ProtocolStream
import perso.stealthnet.core.util.DebugInputStream

object Test {

  def main(args: Array[String]) {
    println("Started")

    Security.addProvider(new BouncyCastleProvider())

    val input = "test".getBytes()
    val rijndael = RijndaelParameters()

    val rijndaelCipher = Ciphers.rijndaelEncrypter(rijndael)
    val rijndaelEncrypted = new ByteArrayOutputStream()
    val rijndaelCipherOutput = new BCCipherOutputStream(rijndaelEncrypted, rijndaelCipher)
    rijndaelCipherOutput.write(input)
    rijndaelCipherOutput.close()
    println(new String(rijndaelEncrypted.toByteArray))

    val rijndaelDecipher = Ciphers.rijndaelDecrypter(rijndael)
    val rijndaelCipherInput = new BCCipherInputStream(new ByteArrayInputStream(rijndaelEncrypted.toByteArray), rijndaelDecipher)
    val buffer = new Array[Byte](32 * 1024)
    val rijndaelDecrypted = rijndaelCipherInput.read(buffer)
    println(new String(buffer, 0, rijndaelDecrypted))

    val rsaCipher = Ciphers.rsaEncrypter(Ciphers.keySpecToKey(new RSAPublicKeySpec(RSAKeys.publicKey.getModulus, RSAKeys.publicKey.getPublicExponent)))
    val rsaEncrypted = new ByteArrayOutputStream()
    val rsaCipherOutput = new CipherOutputStream(rsaEncrypted, rsaCipher)
    rsaCipherOutput.write(input)
    rsaCipherOutput.close()
    println(new String(rsaEncrypted.toByteArray))

    val rsaDecipher = Ciphers.rsaDecrypter(RSAKeys.privateKey)
    val rsaCipherInput = new CipherInputStream(new ByteArrayInputStream(rsaEncrypted.toByteArray), rsaDecipher)
    val rsaDecrypted = rsaCipherInput.read(buffer)
    println(new String(buffer, 0, rsaDecrypted))

    testRSA()
  }

  def testRSA() {
    val modulus = "00c76c63704a37bc954e0059389499476b3d43f0c8f383f17d0bc64baeba1bb8e6269066d8c42ebae4d48ae27c4f8075f34836262bf99409b47035f82352608078f98cad782f3efd272379dc2797cc29b8ca1c4df60b71a0cb2175f7e40642b1e35f8d1e8ce94ae6a3ccafe110ecbbde3f13ac9119687955130d5259655424b937"
    val exponent = "01a88c1725f6dab19f296f9481dfd87132dc3ac761070ade105800cf1e2b16d98b1e8e4c652d424e9ed9dcd24dd2f2a828449302b09ba38d0595dd4f65e2f98e8434b3bddc61ee2dfc63021512efb987b4560f0636fd9dcdd4c306ffca7ea5a24624acb40e6d79944a2749cda9dfa0ccb13a33f27319515fc1806890be0bb109"
    val rsaPrivateKey = KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME).generatePrivate(
      new RSAPrivateKeySpec(new BigInteger(Hash.hexToHash(modulus).bytes), new BigInteger(Hash.hexToHash(exponent).bytes))
    ).asInstanceOf[RSAPrivateKey]
    val rsaEncrypted = "08D2EB60EEE45C1EC53693AA545BBF12175F40A2242ABF3615F775370408A42B3082F78644A5B048DEE93EB3CA2B6499B3B2FCB9755E9110D743A68128790CDF559662EEC3A51A5BE1B52B4EAB79FC0F634B701564ECBDF17BB030D4509B201D0910EF78412F6BEB921150E9A75EBFA41C320943584D69A35FDC4C8E0021C015"

    val buffer = new Array[Byte](32 * 1024)
    val rsaDecipher = Ciphers.rsaDecrypter(rsaPrivateKey)
    val rsaCipherInput = new CipherInputStream(new DebugInputStream(new ByteArrayInputStream(Hash.hexToHash(rsaEncrypted).bytes), List("abc" -> "def")), rsaDecipher)
    ProtocolStream.readByte(rsaCipherInput)
    val rsaDecrypted = rsaCipherInput.read(buffer)
    println(HexDumper.dump(buffer, 0, rsaDecrypted))

    
  }

}
