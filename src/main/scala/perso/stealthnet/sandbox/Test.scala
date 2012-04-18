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
import perso.stealthnet.util.HexDumper
import java.security.KeyFactory
import java.security.spec.RSAPrivateKeySpec
import java.math.BigInteger
import perso.stealthnet.core.cryptography.Hash
import java.security.interfaces.RSAPrivateKey
import perso.stealthnet.network.protocol.ProtocolStream
import perso.stealthnet.util.DebugInputStream
import perso.stealthnet.core.cryptography.CipherMode
import perso.stealthnet.core.cryptography.PaddingMode

object Test {

  def main(args: Array[String]) {
    println("Started")

    Security.addProvider(new BouncyCastleProvider())

    if (false) {
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
    }

    testRSA()
    testRijndael()
  }

  def testRSA() {
    val modulus = "00c76c63704a37bc954e0059389499476b3d43f0c8f383f17d0bc64baeba1bb8e6269066d8c42ebae4d48ae27c4f8075f34836262bf99409b47035f82352608078f98cad782f3efd272379dc2797cc29b8ca1c4df60b71a0cb2175f7e40642b1e35f8d1e8ce94ae6a3ccafe110ecbbde3f13ac9119687955130d5259655424b937"
    val exponent = "01a88c1725f6dab19f296f9481dfd87132dc3ac761070ade105800cf1e2b16d98b1e8e4c652d424e9ed9dcd24dd2f2a828449302b09ba38d0595dd4f65e2f98e8434b3bddc61ee2dfc63021512efb987b4560f0636fd9dcdd4c306ffca7ea5a24624acb40e6d79944a2749cda9dfa0ccb13a33f27319515fc1806890be0bb109"
    val rsaPrivateKey = KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME).generatePrivate(
      new RSAPrivateKeySpec(new BigInteger(Hash.hexToHash(modulus).bytes), new BigInteger(Hash.hexToHash(exponent).bytes))
    ).asInstanceOf[RSAPrivateKey]
    val rsaEncryptedHex = "08D2EB60EEE45C1EC53693AA545BBF12175F40A2242ABF3615F775370408A42B3082F78644A5B048DEE93EB3CA2B6499B3B2FCB9755E9110D743A68128790CDF559662EEC3A51A5BE1B52B4EAB79FC0F634B701564ECBDF17BB030D4509B201D0910EF78412F6BEB921150E9A75EBFA41C320943584D69A35FDC4C8E0021C015"

    val rsaEncrypted = Hash.hexToHash(rsaEncryptedHex).bytes

    val buffer = new Array[Byte](32 * 1024)
    val rsaDecipher = Ciphers.rsaDecrypter(rsaPrivateKey)
    val rsaCipherInput = new CipherInputStream(new ByteArrayInputStream(rsaEncrypted), rsaDecipher)
    val rsaDecrypted = rsaCipherInput.read(buffer)
    println("RSA encrypted:\n" + HexDumper.dump(rsaEncrypted))
    println("RSA decrypted:\n" + HexDumper.dump(buffer, 0, rsaDecrypted))
  }

  def testRijndael() {
    val blockSize = 256
    val feedbackSize = 256
    val keySize = 256
    val cipherMode = CipherMode.CBC
    val paddingMode = PaddingMode.PKCS7
    val iv = HexDumper.undump("""B7 ED C6 5C 50 57 2E BB - 6B DA A1 EB 2B B6 C4 03 | [...\PW..k...+...]
0010: 02 C1 1B 46 43 7F 6A 65 - 15 DF 68 FB FA FB B4 D6""")
    val key = HexDumper.undump("""0000: 64 97 85 F1 06 56 2E 67 - 20 CF 4C 2C 66 50 55 C4 | [d....V.g .L,fPU.]
0010: 23 C8 41 A1 2E F0 35 CE - 07 B9 7A FF 20 EA 96 EE | [#.A...5...z. ...]""")
    val rijndaelEncrypted = HexDumper.undump("""0000: 20 01 D8 23 E9 C0 AD A8 - 70 AF 5A 55 CA DB 9F C9 | [ ..#....p.ZU....]
0010: F3 21 61 63 29 BF 18 3A - FC 66 5E 61 FA B9 19 23 | [.!ac)..:.f^a...#]
0020: F1 16 E1 F0 C8 EC 8B B9 - 42 D8 B8 A0 39 EC FA 53 | [........B...9..S]
0030: 09 01 3B 4F 13 3E 6F B2 - 1A EF C1 D3 1D D2 12 56 | [..;O.>o........V]
0040: D9 98 98 4F 31 68 8D E7 - E1 2C 20 82 02 55 AA 2D | [...O1h..., ..U.-]
0050: BE B2 EF A9 20 74 EC E1 - 04 4F 87 80 52 9E 0A D3 | [.... t...O..R...]
0060: 9A DE 69 6B 18 92 C7 A6 - 4A EC 28 E9 7F 87 4C AF | [..ik....J.(...L.]
0070: D3 82 80 FE CB 11 E6 05 - FE E8 C9 1C 01 6A 1C 0C | [.............j..]
0080: 8B 9B BF D6 3E 7B A7 E1 - 0B 89 CB 58 10 D2 F2 F9 | [....>{.....X....]
0090: 24 C1 73 86 17 40 D6 E3 - 7F 24 13 9C 2F 52 B3 BC | [$.s..@...$../R..]
00A0: AE 79 86 03 25 93 7B AB - A2 3E 52 5D 4C 4B 9F 48 | [.y..%.{..>R]LK.H]
00B0: 22 91 70 BF 78 D7 04 58 - AE 69 3C 0C 0D E8 C7 CA | [".p.x..X.i<.....]
00C0: 6F 15 1B 63 1F 2A AE BE - 0F 3C 8B 74 39 3C E5 04 | [o..c.*...<.t9<..]
00D0: 30 07 42 69 8A E6 7A 06 - A9 14 E0 4E 79 F1 1F 60 | [0.Bi..z....Ny..`]
00E0: DF B5 25 59 E5 F1 7F F8 - CD B6 E8 2E 24 EE 9C 0F | [..%Y........$...]
00F0: 0E A4 61 3D 17 0F E2 94 - E7 5F 6F A3 5A 0D A3 18 | [..a=....._o.Z...]
0100: 6E 9F BD 37 7C 28 44 2C - 39 1D 23 ED 3F C1 6B 0B | [n..7|(D,9.#.?.k.]
0110: 08 50 AA CD 70 D3 E8 0A - 22 9B CC B4 4B 3F 2A 85 | [.P..p..."...K?*.]
        0000: 30 8B""")

    val rijndael = new RijndaelParameters(blockSize, feedbackSize, keySize, cipherMode, paddingMode, key, iv)
    val buffer = new Array[Byte](32 * 1024)
    val rijndaelDecipher = Ciphers.rijndaelDecrypter(rijndael)
    val rijndaelCipherInput = new BCCipherInputStream(new ByteArrayInputStream(rijndaelEncrypted), rijndaelDecipher)
    val rijndaelDecrypted = rijndaelCipherInput.read(buffer)
    println("Rijndael encrypted:\n" + HexDumper.dump(rijndaelEncrypted))
    println("Rijndael decrypted:\n" + HexDumper.dump(buffer, 0, rijndaelDecrypted))
  }

}
