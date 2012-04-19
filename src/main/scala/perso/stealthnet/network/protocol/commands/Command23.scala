package perso.stealthnet.network.protocol.commands

import java.io.InputStream
import perso.stealthnet.core.cryptography.Hash
import perso.stealthnet.network.protocol.{BitSize, Encryption, ProtocolStream}

object Command23 extends CommandBuilder {

  val code: Byte = 0x23

  def read(input: InputStream): Command = {
    val commandId = ProtocolStream.readBytes(input, 48)
    val senderPeerID = ProtocolStream.readBytes(input, 48)
    val receiverPeerID = ProtocolStream.readBytes(input, 48)
    val searchID = ProtocolStream.readBytes(input, 48)

    //new Command23(commandId, senderPeerID, receiverPeerID, searchID)
    /* XXX */
    null
  }

}

class Command23(
  /* 48-bytes */
  val commandId: Hash,
  /* 48-bytes */
  val senderPeerID: Hash,
  /* 48-bytes */
  val receiverPeerID: Hash,
  /* 48-bytes */
  val searchID: Hash,
  val searchResults: List[SearchResult]
) extends Command
{

  val code = SearchCommand.code

  val encryption = Encryption.Rijndael

  assert(commandId != null)
  assert(senderPeerID != null)
  assert(receiverPeerID != null)
  assert(searchID != null)
  assert(searchResults != null)

  def arguments() = List(
    "commandId" -> HashArgument(commandId, 48),
    "senderPeerID" -> HashArgument(senderPeerID, 48),
    "receiverPeerID" -> HashArgument(receiverPeerID, 48),
    "searchID" -> HashArgument(searchID, 48),
    "searchResults" -> ListArgument(searchResults)
  )

}

object SearchResult extends CommandArgumentBuilder[SearchResult] {

  def read(input: InputStream): SearchResult = {
    val fileHash = ProtocolStream.readBytes(input, 64)
    val fileSize = ProtocolStream.readInteger(input, BitSize.Int)
    val fileName = ProtocolStream.readString(input)

    /* XXX */
    null
  }

}

class SearchResult(
  /* 64-bytes */
  val fileHash: Hash,
  val fileSize: Long,
  val fileName: String,
  val metaData: Map[String, String],
  val comment: String,
  val rating: Short
) extends CommandArguments
{

  assert(fileHash != null)
  assert(fileName != null)
  assert(metaData != null)
  assert(comment != null)
  assert((rating >= 0) && (rating <= 3))

  def arguments() = List(
    "fileHash" -> HashArgument(fileHash, 64),
    "fileSize" -> IntegerArgument(fileSize, BitSize.Int),
    "fileName" -> StringArgument(fileName),
    "metaData" -> StringMapArgument(metaData),
    "comment" -> StringArgument(comment),
    "rating" -> ByteArgument(rating.byteValue)
  )

}
