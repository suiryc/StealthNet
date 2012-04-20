package perso.stealthnet.network.protocol.commands

import java.io.InputStream
import perso.stealthnet.cryptography.Hash
import perso.stealthnet.network.protocol.{BitSize, Encryption, ProtocolStream}

object Command54 extends CommandBuilder {

  val code: Byte = 0x54

  def argumentDefinitions = List(
    HashArgumentDefinition("commandId", 48),
    HashArgumentDefinition("senderPeerID", 48),
    HashArgumentDefinition("receiverPeerID", 48),
    HashArgumentDefinition("sourceSearchID", 48),
    IntegerArgumentDefinition("fileSize", BitSize.Int),
    StringArgumentDefinition("fileName"),
    StringMapArgumentDefinition("metaData"),
    StringArgumentDefinition("comment"),
    ByteArgumentDefinition("rating"),
    ByteArrayArgumentDefinition("sectorsMap", BitSize.Short)
  )

  def read(input: InputStream): Command = {
    val arguments = readArguments(input)
    val commandId = arguments("commandId").asInstanceOf[Hash]
    val senderPeerID = arguments("senderPeerID").asInstanceOf[Hash]
    val receiverPeerID = arguments("receiverPeerID").asInstanceOf[Hash]
    val sourceSearchID = arguments("sourceSearchID").asInstanceOf[Hash]
    val fileSize = arguments("fileSize").asInstanceOf[Long]
    val fileName = arguments("fileName").asInstanceOf[String]
    val metaData = arguments("metaData").asInstanceOf[Map[String, String]]
    val comment = arguments("comment").asInstanceOf[String]
    val rating = arguments("rating").asInstanceOf[Byte]
    val sectorsMap = arguments("sectorsMap").asInstanceOf[Array[Byte]]

    new Command54(commandId, senderPeerID, receiverPeerID, sourceSearchID,
      fileSize, fileName, metaData, comment, rating, sectorsMap)
  }

}

class Command54(
  /* 48-bytes */
  val commandId: Hash,
  /* 48-bytes */
  val senderPeerID: Hash,
  /* 48-bytes */
  val receiverPeerID: Hash,
  /* 48-bytes */
  val sourceSearchID: Hash,
  /* unsigned int */
  val fileSize: Long,
  val fileName: String,
  val metaData: Map[String, String],
  val comment: String,
  /* byte */
  val rating: Short,
  val sectorsMap: Array[Byte]
) extends Command
{

  val code = Command54.code

  val encryption = Encryption.Rijndael

  assert(commandId != null)
  assert(senderPeerID != null)
  assert(receiverPeerID != null)
  assert(sourceSearchID != null)
  assert(fileName != null)
  assert(metaData != null)
  assert(comment != null)
  assert((rating >= 0) && (rating <= 3))
  assert(sectorsMap != null)

  def argumentDefinitions = Command54.argumentDefinitions

  def arguments = Map(
    "commandId" -> commandId,
    "senderPeerID" -> senderPeerID,
    "receiverPeerID" -> receiverPeerID,
    "sourceSearchID" -> sourceSearchID,
    "fileSize" -> fileSize,
    "fileName" -> fileName,
    "metaData" -> metaData,
    "comment" -> comment,
    "rating" -> rating.byteValue,
    "sectorsMap" -> sectorsMap
  )

}
