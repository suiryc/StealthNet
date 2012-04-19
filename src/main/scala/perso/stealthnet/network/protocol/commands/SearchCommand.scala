package perso.stealthnet.network.protocol.commands

import java.io.InputStream
import perso.stealthnet.core.cryptography.Hash
import perso.stealthnet.network.protocol.{Encryption, ProtocolStream}

object SearchCommand extends CommandBuilder {

  val code: Byte = 0x20

  def read(input: InputStream): Command = {
    val commandId = ProtocolStream.readBytes(input, 48)
    val floodingHash = ProtocolStream.readBytes(input, 48)
    val senderPeerID = ProtocolStream.readBytes(input, 48)
    val searchID = ProtocolStream.readBytes(input, 48)
    val searchPattern = ProtocolStream.readString(input)

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

  def arguments() = List(
    "commandId" -> HashArgument(commandId, 48),
    "floodingHash" -> HashArgument(floodingHash, 48),
    "senderPeerID" -> HashArgument(senderPeerID, 48),
    "searchID" -> HashArgument(searchID, 48),
    "searchPattern" -> StringArgument(searchPattern)
  )

}
