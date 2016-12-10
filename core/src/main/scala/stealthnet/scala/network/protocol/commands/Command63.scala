package stealthnet.scala.network.protocol.commands

import java.io.InputStream
import stealthnet.scala.Constants
import stealthnet.scala.network.protocol.Encryption
import suiryc.scala.util.Hash

object Command63 extends CommandBuilder {

  val code: Byte = 0x63

  def argumentDefinitions = List(
    HashArgumentDefinition("commandId", Constants.hashLength_48B),
    HashArgumentDefinition("senderPeerID", Constants.hashLength_48B),
    HashArgumentDefinition("receiverPeerID", Constants.hashLength_48B),
    HashArgumentDefinition("sourceSearchID", Constants.hashLength_48B)
  )

  def read(input: InputStream): Command = {
    val arguments = readArguments(input)
    val commandId = arguments("commandId").asInstanceOf[Hash]
    val senderPeerID = arguments("senderPeerID").asInstanceOf[Hash]
    val receiverPeerID = arguments("receiverPeerID").asInstanceOf[Hash]
    val sourceSearchID = arguments("sourceSearchID").asInstanceOf[Hash]

    new Command63(commandId, senderPeerID, receiverPeerID, sourceSearchID)
  }

}

class Command63(
  /* 48-bytes */
  val commandId: Hash,
  /* 48-bytes */
  val senderPeerID: Hash,
  /* 48-bytes */
  val receiverPeerID: Hash,
  /* 48-bytes */
  val sourceSearchID: Hash
) extends Command
{

  val code: Byte = Command63.code

  val encryption = Encryption.Rijndael

  // scalastyle:off null
  assert(commandId != null)
  assert(senderPeerID != null)
  assert(receiverPeerID != null)
  assert(sourceSearchID != null)
  // scalastyle:on null

  def argumentDefinitions: List[HashArgumentDefinition] = Command63.argumentDefinitions

  def arguments = Map(
    "commandId" -> commandId,
    "senderPeerID" -> senderPeerID,
    "receiverPeerID" -> receiverPeerID,
    "sourceSearchID" -> sourceSearchID
  )

}
