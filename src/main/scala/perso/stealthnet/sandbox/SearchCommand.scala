package perso.stealthnet.sandbox

import java.io.ByteArrayOutputStream
import java.net.URI
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.bouncycastle.jce.provider.BouncyCastleProvider
import scala.io.Source
import scala.xml.{Elem,XML}
import perso.stealthnet.core.cryptography.Hash
import perso.stealthnet.core.util.UUID

/**
 * Encryption methods.
 */
object Encryption extends Enumeration {

  /** Available methods. */
  val None, RSA, Rijndael = Value

  def id(v: Encryption.Value): Byte = v match {
    case None => 0x00 byteValue
    case RSA => 0x01 byteValue
    case Rijndael => 0x02 byteValue
  }

}

object Constants {

  val protocol = "LARS REGENSBURGER'S FILE SHARING PROTOCOL 0.2";

}

class SearchCommand {

  def process() {
    val commandId: Hash = UUID.generate()
    val floodingHash: Hash = generateFloodingHash()
    val senderPeerID: Hash = UUID.generate()
    val searchID: Hash = UUID.generate()
    val searchPattern: String = "intouchables"

  }

  def build(commandId: Hash, floodingHash: Hash, senderPeerID: Hash, searchID: Hash, searchPattern: String) {
    val dataStream = (new DataStream)
        .write(0x20 byteValue)
        .write(commandId)
        .write(floodingHash)
        .write(senderPeerID)
        .write(searchID)
        .write(searchPattern)

    val commandBuilder = new CommandBuilder(Encryption.Rijndael, 0x20 byteValue, dataStream)
  }

  /* http://www.scribd.com/doc/28681327/69/Stealthnet-decloaked */
  def generateFloodingHash(): Hash = {
    var hash: Hash = UUID.generate()

    while (hash.bytes(47) <= 51) {
      hash = UUID.generate()
    }

    hash
  }

}

class DataStream {

  private val baos = new ByteArrayOutputStream()

  def write(b: Byte): DataStream = {
    baos.write(b)
    this
  }

  def write(s: Short): DataStream = {
    for (idx <- 0 to 1)
      baos.write(((s >>> (8 * idx)) & 0xFF).byteValue)
    this
  }

  def write(i: Int): DataStream = {
    for (idx <- 0 to 3)
      baos.write(((i >>> (8 * idx)) & 0xFF).byteValue)
    this
  }

  def write(str: String): DataStream = {
    val bytes = str.getBytes("UTF-8")

    if ((bytes.length & 0xFFFF) != 0)
      throw new IllegalArgumentException("String[%s] length exceeds capacity".format(str))

    baos.write(bytes.length.shortValue)
    baos.write(bytes)
    this
  }

  /* XXX - really useful (other than for procotol header) ? */
  def writeAscii(str: String): DataStream = {
    val bytes = str.getBytes("US-ASCII")

    if ((bytes.length & 0xFFFF) != 0)
      throw new IllegalArgumentException("String[%s] length exceeds capacity".format(str))

    baos.write(bytes.length.shortValue)
    baos.write(bytes)
    this
  }

  def write(hash: Hash): DataStream = {
    baos.write(hash.bytes)
    this
  }

  def getBytes(): Array[Byte] = baos.toByteArray

}

class CommandBuilder(val encryption: Encryption.Value, val code: Byte, val dataStream: DataStream) {

  def send(/* connection */) = {
    /* XXX */
    val data: Array[Byte] = encryption match {
      case Encryption.None => dataStream.getBytes
      case Encryption.RSA => dataStream.getBytes
      /* encryptedData = Core.RSAEncrypt(connection.PublicKey, m_CommandData.ToArray()); */
      case Encryption.Rijndael => dataStream.getBytes
      /* encryptedData = Core.RijndaelEncrypt(connection.SendingKey, m_CommandData.ToArray()); */
    }

    if ((data.length & 0xFFFF) != 0)
      throw new IllegalArgumentException("Command[%d] length[%d] exceeds capacity".format(code.intValue, data.length))

    val command = (new DataStream)
        .writeAscii(Constants.protocol)
        .write(Encryption.id(encryption))
        .write(data.length.shortValue)
        .write(data)

    /* connection.send(command.getBytes(), code) */
  }

}

object Test {
  def main(args: Array[String]): Unit = {
    println("Started")
    //val keygen = KeyGenerator.getInstance("Rijndael");
    //keygen.init(256);
    //val cipher = Cipher.getInstance("Rijndael/CBC/PKCS7Padding")

    /* XXX - JCE Unlimited Strength Jurisdiction Policy Files are needed for keys > 128-bits */
    Security.addProvider(new BouncyCastleProvider())

    val input = "test".getBytes()
    //val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
    //val spec = new PBEKeySpec("abcdef".toCharArray(), "ghij".getBytes(), 65536, 256);
    //val tmpKey = factory.generateSecret(spec);
    //val key = /*Array[Byte]()*/tmpKey.getEncoded()
    val key: Array[Byte] = Hash.hashToBytes(Hash("2957f6d6d9c71d97d31af3d3b765c92d4a8ca665142c9b90364496c1ab7be2f3"))
    println(Hash.bytesToHash(key))
    val keySpec = new SecretKeySpec(key, "Rijndael");
    //val keySpec = new SecretKeySpec(key, "AES");
    val IV: Array[Byte] = Hash.hashToBytes(Hash("976697d73a33759dac654cb06461bb23"))
    val ivSpec = new IvParameterSpec(IV);
    val cipher = Cipher.getInstance("Rijndael/CBC/PKCS7Padding", BouncyCastleProvider.PROVIDER_NAME);
    //val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
    val output = cipher.doFinal(input)
    println(Hash.bytesToHash(output))

    val decipher = Cipher.getInstance("Rijndael/CBC/PKCS7Padding", BouncyCastleProvider.PROVIDER_NAME);
    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
    val decrypted = cipher.doFinal(output)
    println(new String(decrypted))


    /*
    WebCacheClient.getPeer("http://rshare.de/rshare.asmx") match {
      case Some(peer) => println(peer)
      case None =>
    }

    WebCacheClient.getPeer("http://webcache.stealthnet.at/rwpmws.php") match {
      case Some(peer) => println(peer)
      case None =>
    }
    */
  }

}

object SoapClient {

  def wrap(elem: Elem): String = {
    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"  + <soap12:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
  <soap12:Body>
    { elem }
  </soap12:Body>
</soap12:Envelope>
  }

  def doRequest(host: String, req: Elem): Either[String, Elem] = {
    val url = new java.net.URL(host)
    val outs = wrap(req).getBytes("UTF-8")
    val conn = url.openConnection.asInstanceOf[java.net.HttpURLConnection]
    try {
      conn.setRequestMethod("POST")
      conn.setDoOutput(true)
      conn.setRequestProperty("Content-Length", outs.length.toString)
      conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8")
      conn.getOutputStream.write(outs)
      conn.getOutputStream.close
      Right(XML.load(conn.getInputStream))
    }
    catch {
      case e: Exception =>
        val response = Source.fromInputStream(conn.getErrorStream).mkString
        val details = try {
          val doc = XML.loadString(response)
          "Fault code[" + (doc \\ "faultcode").text +
              "] actor[" + (doc \\ "faultactor").text +
              "]: " + (doc \\ "faultstring").text
        }
        catch {
          case e: Exception => response
        }

        Left("Response code[" + conn.getResponseCode +
            "] message[" + conn.getResponseMessage +
            "] details[" + details + "]")
      }
  }
}

object WebCacheClient {

  def getPeer(host: String): Option[String] = {
    SoapClient.doRequest(host, <GetPeer xmlns="http://rshare.de/rshare.asmx" />) match {
      case Left(l) =>
        println("Failed: " + l)
        None

      case Right(r) =>
        r.text match {
          case "" =>
            println("No peer received")
            None

          case peer =>
            Some(peer)
        }
    }
  }

}
