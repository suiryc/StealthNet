package stealthnet.scala.network.protocol.commands

import java.io.InputStream
import stealthnet.scala.cryptography.Hash
import stealthnet.scala.network.protocol.{BitSize, Encryption, ProtocolStream}

object Command23 extends CommandBuilder {

  val code: Byte = 0x23

  def argumentDefinitions = List(
    HashArgumentDefinition("commandId", 48),
    HashArgumentDefinition("senderPeerID", 48),
    HashArgumentDefinition("receiverPeerID", 48),
    HashArgumentDefinition("searchID", 48),
    ListArgumentsDefinition("searchResults", SearchResult)
  )

  def read(input: InputStream): Command = {
    val arguments = readArguments(input)
    val commandId = arguments("commandId").asInstanceOf[Hash]
    val senderPeerID = arguments("senderPeerID").asInstanceOf[Hash]
    val receiverPeerID = arguments("receiverPeerID").asInstanceOf[Hash]
    val searchID = arguments("searchID").asInstanceOf[Hash]
    val searchResults = arguments("searchResults").asInstanceOf[List[SearchResult]]

    new Command23(commandId, senderPeerID, receiverPeerID, searchID, searchResults)
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

  val code = Command23.code

  val encryption = Encryption.Rijndael

  assert(commandId != null)
  assert(senderPeerID != null)
  assert(receiverPeerID != null)
  assert(searchID != null)
  assert(searchResults != null)

  def argumentDefinitions = Command23.argumentDefinitions

  def arguments = Map(
    "commandId" -> commandId,
    "senderPeerID" -> senderPeerID,
    "receiverPeerID" -> receiverPeerID,
    "searchID" -> searchID,
    "searchResults" -> searchResults
  )

}

/**
 * Search result companion object.
 */
object SearchResult
  extends CommandArgumentsReader[SearchResult]
  with CommandArgumentDefinitions
{

  def argumentDefinitions = List(
    HashArgumentDefinition("fileHash", 64),
    IntegerArgumentDefinition("fileSize", BitSize.Int),
    StringArgumentDefinition("fileName"),
    StringMapArgumentDefinition("metaData"),
    StringArgumentDefinition("comment"),
    ByteArgumentDefinition("rating")
  )

  def read(input: InputStream): SearchResult = {
    val arguments = readArguments(input)
    val fileHash = arguments("fileHash").asInstanceOf[Hash]
    val fileSize = arguments("fileSize").asInstanceOf[Long]
    val fileName = arguments("fileName").asInstanceOf[String]
    val metaData = arguments("metaData").asInstanceOf[Map[String, String]]
    val comment = arguments("comment").asInstanceOf[String]
    val rating = arguments("rating").asInstanceOf[Byte]

    new SearchResult(fileHash, fileSize, fileName, metaData, comment, rating)
  }

}

/**
 * Search result.
 *
 * @todo Use enumeration for rating.
 */
class SearchResult(
  /* 64-bytes */
  val fileHash: Hash,
  /* unsigned int */
  val fileSize: Long,
  val fileName: String,
  val metaData: Map[String, String],
  val comment: String,
  /* byte */
  val rating: Short
) extends CommandArguments
{

  assert(fileHash != null)
  assert(fileName != null)
  assert(metaData != null)
  assert(comment != null)
  assert((rating >= 0) && (rating <= 3))

  def argumentDefinitions = SearchResult.argumentDefinitions

  def arguments = Map(
    "fileHash" -> fileHash,
    "fileSize" -> fileSize,
    "fileName" -> fileName,
    "metaData" -> metaData,
    "comment" -> comment,
    "rating" -> rating.byteValue
  )

}
