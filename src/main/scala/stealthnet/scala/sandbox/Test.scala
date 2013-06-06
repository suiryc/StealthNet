package stealthnet.scala.sandbox

import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import stealthnet.scala.cryptography.Ciphers
import stealthnet.scala.cryptography.RijndaelParameters
import stealthnet.scala.cryptography.io.BCCipherOutputStream
import java.io.ByteArrayOutputStream
import stealthnet.scala.cryptography.io.BCCipherInputStream
import java.io.ByteArrayInputStream
import stealthnet.scala.cryptography.RSAKeys
import javax.crypto.CipherOutputStream
import javax.crypto.CipherInputStream
import java.security.spec.RSAPublicKeySpec
import stealthnet.scala.util.HexDumper
import java.security.KeyFactory
import java.security.spec.RSAPrivateKeySpec
import java.math.BigInteger
import stealthnet.scala.util.Hash
import java.security.interfaces.RSAPrivateKey
import stealthnet.scala.network.protocol.ProtocolStream
import stealthnet.scala.util.io.DebugInputStream
import stealthnet.scala.cryptography.CipherMode
import stealthnet.scala.cryptography.PaddingMode
import stealthnet.scala.network.protocol.commands.Command

// scalastyle:off line.size.limit regex
object Test {

  def main(args: Array[String]) {
    println("Started")

    Security.addProvider(new BouncyCastleProvider())

    if (false) {
    val test = RijndaelParameters()
    println(test)
    println(RSAKeys.privateKey)
    return
    }

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
    testCommandBuilder()
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
    val iv = HexDumper.undump("""0000: 1C CC 54 C6 88 85 37 88 - 8E 91 A8 19 0D 8B 56 8A | [..T...7.......V.]
0010: D6 38 A8 17 A3 7E 62 A4 - 44 28 A2 76 3F 69 69 16 | [.8...~b.D(.v?ii.]""")
    val key = HexDumper.undump("""0000: 7F 01 E4 AA D3 D4 F7 AF - 48 C9 75 48 CD D3 B6 F8 | [........H.uH....]
0010: 75 08 3E BB D4 F3 85 DB - 34 11 1A 4F 6F CB F5 74 | [u.>.....4..Oo..t]""")
    val rijndaelEncrypted = HexDumper.undump("""0000: D6 B6 3E 0D C0 A6 - AF 79 CD 1E C4 7D 23 EE | [....>....y...}#.]
0010: 9E B6 2C 9D 3D 7E 30 D8 - E8 7E 16 4A 69 16 46 E3 | [..,.=~0..~.Ji.F.]
0020: 19 98 70 2E 0D A6 4A 1D - E2 9F C8 30 56 7C E7 4E | [..p...J....0V|.N]
0030: 18 8F 2B 63 79 43 8D A1 - 20 DC 79 A5 8B C7 62 78 | [..+cyC.. .y...bx]
0040: 7E 36 C5 B6 9A EC FA F5 - 25 5A 1B F8 A4 02 F2 44 | [~6......%Z.....D]
0050: 76 78 04 3A 80 32 32 24 - 2C 34 6D C3 8D 26 8B 41 | [vx.:.22$,4m..&.A]
0060: A0 27 CC 30 31 A3 FB BB - 57 4B 14 54 FC 55 28 6A | [.'.01...WK.T.U(j]
0070: FB 10 6F 20 21 3D 58 9C - 12 9F 8D 73 B3 F0 53 D1 | [..o !=X....s..S.]
0080: 60 EC F4 ED 04 54 03 BE - 73 2A 92 59 9F 17 22 09 | [`....T..s*.Y..".]
0090: CF 17 5F 34 67 68 82 35 - DC F4 7D FE BF 6C 4C 9A | [.._4gh.5..}..lL.]
00A0: F5 58 7C C0 20 2F 96 D5 - B1 D4 C5 44 06 AD B9 26 | [.X|. /.....D...&]
00B0: 6A 7B 2E DC 45 5C DD DD - 8A B7 78 56 8B 87 37 78 | [j{..E\....xV..7x]
00C0: B4 D2 F4 74 02 DD A5 B8 - E7 5D 9F 28 FC 0A E3 DA | [...t.....].(....]
00D0: 81 D1 22 B9 CF 10 40 AD - C9 33 BC 97 CF 21 1B 24 | [.."...@..3...!.$]
00E0: C2 C5 CB 60 8F 5B 54 CA - BE 93 86 CB 86 32 D7 AF | [...`.[T......2..]
00F0: C1 11 1C 87 59 90 36 72 - 9A 98 EF 93 F8 C7 DD 01 | [....Y.6r........]
0100: 7D 92 87 97 7B 8F B1 0B - 6A B3 04 A0 31 6D C5 69 | [}...{...j...1m.i]
0110: 53 CB 86 75 D3 92 5F DE - F9 24 74 87 CB 76 69 1D | [S..u.._..$t..vi.]
0120: BA 2C 11 D0 6F CC D5 9C - B3 F6 83 B7 A7 26 62 FE | [.,..o........&b.]
0130: 23 1D B4 C8 6D 0A 56 36 - BD 28 27 E0 D0 C3 26 71 | [#...m.V6.('...&q]
0140: 00 77 2F AF 25 18 F3 11 - 01 74 1F 80 DC 3A B0 E2 | [.w/.%....t...:..]
0150: D5 83 33 06 59 05 97 25 - F6 F8 DF 62 5B 37 0B F1 | [..3.Y..%...b[7..]
0160: AA A5 F5 7F 18 C9 FD FA - D7 14 D1 75 D1 13 CF 14 | [...........u....]
0170: FA 5C 2C 41 ED A3 95 E1 - 56 C6 62 31 B9 63 3C 0A | [.\,A....V.b1.c<.]
0180: F2 D3 8E 85 5A 7A D6 9B - F4 D1 51 76 5D 6B 0A C8 | [....Zz....Qv]k..]
0190: 06 92 D1 3E CE 71 27 10 - F5 95 85 AC 2F B2 92 9A | [...>.q'...../...]
01A0: D8 70 BF 33 08 09 3D 47 - 93 12 60 B7 E1 11 FC AF | [.p.3..=G..`.....]
01B0: A6 32 2A 86 C5 25 88 57 - 50 6B FD 8D EF 73 E1 B9 | [.2*..%.WPk...s..]
01C0: 0B 44 BC 51 BB C5 A9 08 - DF 32 34 30 AA 1B 99 01 | [.D.Q.....240....]
01D0: 8C FC A7 AA DB 9B 1F B5 - 0D FF 79 EF AD 10 7E 4C | [..........y...~L]
0000: 33 9A""")

    val rijndael = new RijndaelParameters(blockSize, feedbackSize, keySize, cipherMode, paddingMode, key, iv)
    val buffer = new Array[Byte](32 * 1024)
    val rijndaelDecipher = Ciphers.rijndaelDecrypter(rijndael)
    val rijndaelCipherInput = new BCCipherInputStream(new ByteArrayInputStream(rijndaelEncrypted), rijndaelDecipher)
    val rijndaelDecrypted = rijndaelCipherInput.read(buffer)
    println("Rijndael encrypted:\n" + HexDumper.dump(rijndaelEncrypted))
    println("Rijndael decrypted:\n" + HexDumper.dump(buffer, 0, rijndaelDecrypted))
  }

  def testCommandBuilder() {
    val buffer = HexDumper.undump("""0000: 23 09 B9 AD A2 36 CB 85 - 85 57 FF 34 88 93 FE CA | [#....6...W.4....]
0010: 6D 6B 10 1D A1 59 9D 12 - 74 9A 7B E0 3B D1 7D 58 | [mk...Y..t.{.;.}X]
0020: 63 EE F2 D0 D4 E2 CB 1D - FD 4E 78 82 A0 B8 DC A0 | [c........Nx.....]
0030: 31 D9 FB C9 7F D4 BB 17 - 9B 9B 28 FA ED 3A 99 66 | [1.........(..:.f]
0040: C9 BE 3A E7 25 C2 67 F1 - FC D3 DC 4F 55 B1 2B 47 | [..:.%.g....OU.+G]
0050: 33 8B F0 1A A4 E2 30 75 - 00 AF 1A F0 25 73 1C 84 | [3.....0u....%s..]
0060: 52 1D C4 86 AA 3C F1 9A - BB 5E 9C 67 3A 25 25 61 | [R....<...^.g:%%a]
0070: 71 64 C6 56 E1 A8 0B 24 - 5D 01 88 00 56 B5 1D 15 | [qd.V...$]...V...]
0080: 4A F4 66 0F D1 6F 86 CA - F3 FE A2 1D DA 2B E0 C3 | [J.f..o.......+..]
0090: D4 1E BE 0C 24 54 F8 5D - EE 20 DF FB 76 DB E3 8E | [....$T.]. ..v...]
00A0: 1F 89 32 6B 3A B7 F7 5F - 61 8D EA EB CB 17 A0 E4 | [..2k:.._a.......]
00B0: A8 DD C0 57 7E FF FE 47 - F4 2C D2 1E 8A 16 CA 23 | [...W~..G.,.....#]
00C0: E9 01 00 15 0C 77 C5 40 - BD 6B 05 42 31 6A 5E 86 | [.....w.@.k.B1j^.]
00D0: 40 88 8F 46 86 83 93 01 - 84 F0 34 78 BE 10 AA E9 | [@..F......4x....]
00E0: 12 32 2D 75 02 72 AB 5D - 18 6F 46 96 B7 30 5B 06 | [.2-u.r.].oF..0[.]
00F0: CE 09 94 75 7F 29 ED 7C - B9 BB 52 F0 CF D4 11 A6 | [...u.).|..R.....]
0100: 3E B9 CD 00 70 BE 2B 33 - 00 49 6E 74 6F 75 63 68 | [>...p.+3.Intouch]
0110: 61 62 6C 65 73 2E 32 30 - 31 31 2E 46 52 45 4E 43 | [ables.2011.FRENC]
0120: 48 2E 44 56 44 52 69 50 - 2E 58 76 69 44 2D 42 4C | [H.DVDRiP.XviD-BL]
0130: 4F 4F 44 59 4D 41 52 59 - 2E 61 76 69 00 00 00 00 | [OODYMARY.avi....]
0140: 00 00 00 00 00 00 00 00 - 00 00 00 00 00 00 00 00 | [................]
0150: 00 00 00 00 00 00 00 00 - 00 00 00 00 00 00 00 00 | [................]
0160: 00 00 00 00 00 00 00 00 - 00 00 00 00 00 00 00 00 | [................]
0170: 00 00 00 00 00 00 00 00 - 00 00 00 00 00 00 00 00 | [................]""")
    val input = if (false)
        new DebugInputStream(new ByteArrayInputStream(buffer), Nil)
      else
        new ByteArrayInputStream(buffer)
    val code = ProtocolStream.readByte(input)
    Command.commandBuilder(code) match {
      case Some(builder) =>
        try {
          val command = builder.read(input)
          println(s"Command: $command")
        }
        catch {
          case e: Throwable =>
            println("Builder for command[%02X] failed!".format(code))
            e.printStackTrace()
        }

      case None =>
        println("No builder for command[%02X]".format(code))
    }
  }

}
// scalastyle:on line.size.limit regex
