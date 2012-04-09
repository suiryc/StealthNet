package perso.stealthnet.network.protocol

import com.weiglewilczek.slf4s.Logging
import perso.stealthnet.core.util.DataStream
import perso.stealthnet.network.StealthNetConnection
import java.io.OutputStream
import perso.stealthnet.core.cryptography.Hash

/* XXX - trait, or abstract class ? */
trait Command extends Logging {

  val code: Byte
  val encryption: Encryption.Value
  var dataStream: DataStream = null

  def arguments(): List[Any]

  def build(): DataStream = {
    synchronized {
      if (dataStream != null)
        return dataStream
      dataStream = new DataStream()

      for (argument <- arguments) {
        if (argument == null) {
          dataStream = null
          throw new IllegalArgumentException("Missing command argument in " + toString())
        }

        dataStream write argument
      }
    }

    dataStream
  }

  def send(output: OutputStream) = {
    for (argument <- arguments) {
      if (argument == null)
        throw new IllegalArgumentException("Missing command argument in " + toString())

      dataStream write argument
    }
  }

  def send(connection: StealthNetConnection): Unit = {
    assert(connection != null)

    /* XXX */
    logger debug("Sending command[%s] to host[%s]".format(this, connection.host))
    val dataStream = try {
      build()
    }
    catch {
      case e: Exception =>
        logger error("Cannot send command[%02X] to host[%s]".format(code, connection.host), e)
        return
    }

    /* XXX */
    val data: Array[Byte] = encryption match {
      case Encryption.None => dataStream.getBytes
      case Encryption.RSA => dataStream.getBytes
      /* encryptedData = Core.RSAEncrypt(connection.PublicKey, m_CommandData.ToArray()); */
      case Encryption.Rijndael => dataStream.getBytes
      /* encryptedData = Core.RijndaelEncrypt(connection.SendingKey, m_CommandData.ToArray()); */
    }

    if (data.length > 0xFFFF)
       logger error("Cannot send command[%02X] to host[%s]: length[%d] exceeds capacity".
         format(code, connection.host, data.length))

    val command = (new DataStream)
        .writeAscii(Constants.protocol)
        .write(Encryption.id(encryption))
        .write(data.length.shortValue)
        .write(data)

    /* connection.send(command.getBytes(), code) */
  }

  override def toString =
    getClass.getSimpleName + arguments

}
