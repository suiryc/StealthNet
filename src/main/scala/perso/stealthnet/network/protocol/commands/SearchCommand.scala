package perso.stealthnet.network.protocol.commands

import java.io.InputStream
import perso.stealthnet.cryptography.Hash
import perso.stealthnet.network.protocol.{Encryption, ProtocolStream}

object SearchCommand extends CommandBuilder {

  val code: Byte = 0x20

  def argumentDefinitions = List(
    HashArgumentDefinition("commandId", 48),
    HashArgumentDefinition("floodingHash", 48),
    HashArgumentDefinition("senderPeerID", 48),
    HashArgumentDefinition("searchID", 48),
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

  def apply(searchPattern: String) = new SearchCommand(
    commandId = Command.generateId(),
    floodingHash = generateFloodingHash(),
    senderPeerID = Command.generateId(),
    searchID = Command.generateId(),
    searchPattern = searchPattern
  )

  /* http://www.scribd.com/doc/28681327/69/Stealthnet-decloaked */
  def generateFloodingHash(): Hash = {
    var hash: Hash = Command.generateId()

    while (hash.bytes(47) <= 51) {
      hash = Command.generateId()
    }

    hash
  }

}

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

  assert(commandId != null)
  assert(floodingHash != null)
  assert(senderPeerID != null)
  assert(searchID != null)
  assert(searchPattern != null)

  def argumentDefinitions = SearchCommand.argumentDefinitions

  def arguments = Map(
    "commandId" -> commandId,
    "floodingHash" -> floodingHash,
    "senderPeerID" -> senderPeerID,
    "searchID" -> searchID,
    "searchPattern" -> searchPattern
  )

}
