package perso.stealthnet.network.protocol

import perso.stealthnet.core.util.{DataStream, UUID}
import perso.stealthnet.core.cryptography.{Algorithm, Hash, Message}
import perso.stealthnet.network.StealthNetConnection

object SearchCommand {

  def generateId(): Hash = Message.hash(UUID.generate().bytes, Algorithm.SHA384)

  def apply() = new SearchCommand()

  def apply(searchPattern: String) = new SearchCommand(
    commandId = generateId(),
    floodingHash = generateFloodingHash(),
    senderPeerID = generateId(),
    searchID = generateId(),
    searchPattern = searchPattern
  )

  /* http://www.scribd.com/doc/28681327/69/Stealthnet-decloaked */
  def generateFloodingHash(): Hash = {
    var hash: Hash = generateId()

    println(hash)
    println(hash.bytes.size)
    while (hash.bytes(47) <= 51) {
      hash = generateId()
    }

    hash
  }

}

class SearchCommand(
    var commandId: Hash = null,
    var floodingHash: Hash = null,
    var senderPeerID: Hash = null,
    var searchID: Hash = null,
    var searchPattern: String = null
) extends Command {

  val code = 0x20 byteValue
  val encryption = Encryption.Rijndael

  def arguments(): List[Any] =
    List(commandId, floodingHash, senderPeerID, searchID, searchPattern)

}
