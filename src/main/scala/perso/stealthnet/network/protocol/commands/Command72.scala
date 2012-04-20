package perso.stealthnet.network.protocol.commands

import java.io.InputStream
import perso.stealthnet.cryptography.Hash
import perso.stealthnet.network.protocol.{Encryption, ProtocolStream}

object Command72 extends CommandBuilder {

  val code: Byte = 0x72

  def argumentDefinitions = List(
    HashArgumentDefinition("commandId", 48),
    HashArgumentDefinition("senderPeerID", 48),
    HashArgumentDefinition("receiverPeerID", 48),
    HashArgumentDefinition("feedbackID", 48)
  )

  def read(input: InputStream): Command = {
    val arguments = readArguments(input)
    val commandId = arguments("commandId").asInstanceOf[Hash]
    val senderPeerID = arguments("senderPeerID").asInstanceOf[Hash]
    val receiverPeerID = arguments("receiverPeerID").asInstanceOf[Hash]
    val feedbackID = arguments("feedbackID").asInstanceOf[Hash]

    new Command72(commandId, senderPeerID, receiverPeerID, feedbackID)
  }

}

class Command72(
  /* 48-bytes */
  val commandId: Hash,
  /* 48-bytes */
  val senderPeerID: Hash,
  /* 48-bytes */
  val receiverPeerID: Hash,
  /* 48-bytes */
  val feedbackID: Hash
) extends Command
{

  val code = Command72.code

  val encryption = Encryption.Rijndael

  assert(commandId != null)
  assert(senderPeerID != null)
  assert(receiverPeerID != null)
  assert(feedbackID != null)

  def argumentDefinitions = Command72.argumentDefinitions

  def arguments = Map(
    "commandId" -> commandId,
    "senderPeerID" -> senderPeerID,
    "receiverPeerID" -> receiverPeerID,
    "feedbackID" -> feedbackID
  )

}
