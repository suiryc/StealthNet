package stealthnet.scala.network.protocol.commands

import java.io.InputStream
import stealthnet.scala.Constants
import stealthnet.scala.network.protocol.{Encryption, ProtocolStream}
import stealthnet.scala.util.Hash

/**
 * Search command companion object.
 */
object SearchCommand extends CommandBuilder {

  val code: Byte = 0x20

  def argumentDefinitions = List(
    HashArgumentDefinition("commandId", Constants.hashLength_48B),
    HashArgumentDefinition("floodingHash", Constants.hashLength_48B),
    HashArgumentDefinition("senderPeerID", Constants.hashLength_48B),
    HashArgumentDefinition("searchID", Constants.hashLength_48B),
    StringArgumentDefinition("searchPattern")
  )

  def read(input: InputStream): Command = {
    val arguments = readArguments(input)
    val commandId = arguments("commandId").asInstanceOf[Hash]
    val floodingHash = arguments("floodingHash").asInstanceOf[Hash]
    val senderPeerID = arguments("senderPeerID").asInstanceOf[Hash]
    val searchID = arguments("searchID").asInstanceOf[Hash]
    val searchPattern = arguments("searchPattern").asInstanceOf[String]

    new SearchCommand(commandId, floodingHash, senderPeerID, searchID,
      searchPattern)
  }

  /**
   * Factory method from search pattern.
   *
   * @param searchPattern search pattern
   * @return new [[stealthnet.scala.network.protocol.commands.SearchCommand]]
   *   for the given search pattern
   */
  def apply(searchPattern: String) = new SearchCommand(
    commandId = Command.generateId(),
    floodingHash = generateFloodingHash(),
    senderPeerID = Command.generateId(),
    searchID = Command.generateId(),
    searchPattern = searchPattern
  )

  /**
   * Generates a new flooding hash.
   *
   * The flooding hash needs special care.
   *
   * @see [[http://www.scribd.com/doc/28681327/69/Stealthnet-decloaked]]
   */
  def generateFloodingHash(): Hash = {
    var hash: Hash = Command.generateId()

    // scalastyle:off magic.number
    while (hash.bytes(47) <= 51) {
    // scalastyle:on magic.number
      hash = Command.generateId()
    }

    hash
  }

}

/**
 * Search command.
 *
 * Command code: `0x20`
 */
class SearchCommand(
  /* 48-bytes */
  val commandId: Hash,
  /* 48-bytes */
  val floodingHash: Hash,
  /* 48-bytes */
  val senderPeerID: Hash,
  /* 48-bytes */
  val searchID: Hash,
  val searchPattern: String
) extends Command
{

  val code = SearchCommand.code

  val encryption = Encryption.Rijndael

  // scalastyle:off null
  assert(commandId != null)
  assert(floodingHash != null)
  assert(senderPeerID != null)
  assert(searchID != null)
  assert(searchPattern != null)
  // scalastyle:on null

  def argumentDefinitions = SearchCommand.argumentDefinitions

  def arguments = Map(
    "commandId" -> commandId,
    "floodingHash" -> floodingHash,
    "senderPeerID" -> senderPeerID,
    "searchID" -> searchID,
    "searchPattern" -> searchPattern
  )

}
