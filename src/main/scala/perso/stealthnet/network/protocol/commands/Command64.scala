package perso.stealthnet.network.protocol.commands

import java.io.InputStream
import perso.stealthnet.core.cryptography.Hash
import perso.stealthnet.network.protocol.{BitSize, Encryption, ProtocolStream}

object Command64 extends CommandBuilder {

  val code: Byte = 0x64

  def argumentDefinitions = List(
    HashArgumentDefinition("commandId", 48),
    HashArgumentDefinition("senderPeerID", 48),
    HashArgumentDefinition("receiverPeerID", 48),
    HashArgumentDefinition("sourceSearchID", 48),
    ByteArrayArgumentDefinition("sectorsMap", BitSize.Short)
  )

  def read(input: InputStream): Command = {
    val arguments = readArguments(input)
    val commandId = arguments("commandId").asInstanceOf[Hash]
    val senderPeerID = arguments("senderPeerID").asInstanceOf[Hash]
    val receiverPeerID = arguments("receiverPeerID").asInstanceOf[Hash]
    val sourceSearchID = arguments("sourceSearchID").asInstanceOf[Hash]
    val sectorsMap = arguments("sectorsMap").asInstanceOf[Array[Byte]]

    new Command64(commandId, senderPeerID, receiverPeerID, sourceSearchID,
      sectorsMap)
  }

}

class Command64(
  /* 48-bytes */
  val commandId: Hash,
  /* 48-bytes */
  val senderPeerID: Hash,
  /* 48-bytes */
  val receiverPeerID: Hash,
  /* 48-bytes */
  val sourceSearchID: Hash,
  val sectorsMap: Array[Byte]
) extends Command
{

  val code = Command64.code

  val encryption = Encryption.Rijndael

  assert(commandId != null)
  assert(senderPeerID != null)
  assert(receiverPeerID != null)
  assert(sourceSearchID != null)
  assert(sectorsMap != null)

  def argumentDefinitions = Command64.argumentDefinitions

  def arguments = Map(
    "commandId" -> commandId,
    "senderPeerID" -> senderPeerID,
    "receiverPeerID" -> receiverPeerID,
    "sourceSearchID" -> sourceSearchID,
    "sectorsMap" -> sectorsMap
  )

}
