package perso.stealthnet.network.protocol

import java.io.InputStream
import perso.stealthnet.core.cryptography.Hash

object SearchCommand extends CommandBuilder {

  val code = 0x20 byteValue

  def read(input: InputStream): Command = {
    val commandId = ProtocolStream.readHash(input)
    val floodingHash = ProtocolStream.readHash(input)
    val senderPeerID = ProtocolStream.readHash(input)
    val searchID = ProtocolStream.readHash(input)
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
    val commandId: Hash,
    val floodingHash: Hash,
    val senderPeerID: Hash,
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

  def arguments(): List[(String, Any)] = List(
    "commandId" -> commandId,
    "floodingHash" -> floodingHash,
    "senderPeerID" -> senderPeerID,
    "searchID" -> searchID,
    "searchPattern" -> searchPattern
  )

}
