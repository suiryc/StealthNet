package perso.stealthnet.network.protocol.commands

import java.io.InputStream
import perso.stealthnet.core.cryptography.Hash
import perso.stealthnet.network.protocol.{BitSize, Encryption, ProtocolStream}

object Command71 extends CommandBuilder {

  val code: Byte = 0x71

  def argumentDefinitions = List(
    HashArgumentDefinition("commandId", 48),
    HashArgumentDefinition("senderPeerID", 48),
    HashArgumentDefinition("receiverPeerID", 48),
    HashArgumentDefinition("feedbackID", 48),
    IntegerArgumentDefinition("queueLength", BitSize.Int)
  )

  def read(input: InputStream): Command = {
    val arguments = readArguments(input)
    val commandId = arguments("commandId").asInstanceOf[Hash]
    val senderPeerID = arguments("senderPeerID").asInstanceOf[Hash]
    val receiverPeerID = arguments("receiverPeerID").asInstanceOf[Hash]
    val feedbackID = arguments("feedbackID").asInstanceOf[Hash]
    val queueLength = arguments("queueLength").asInstanceOf[Long]

    new Command71(commandId, senderPeerID, receiverPeerID, feedbackID,
      queueLength)
  }

}

class Command71(
  /* 48-bytes */
  val commandId: Hash,
  /* 48-bytes */
  val senderPeerID: Hash,
  /* 48-bytes */
  val receiverPeerID: Hash,
  /* 48-bytes */
  val feedbackID: Hash,
  /* unsigned int */
  val queueLength: Long
) extends Command
{

  val code = Command71.code

  val encryption = Encryption.Rijndael

  assert(commandId != null)
  assert(senderPeerID != null)
  assert(receiverPeerID != null)
  assert(feedbackID != null)

  def argumentDefinitions = Command71.argumentDefinitions

  def arguments = Map(
    "commandId" -> commandId,
    "senderPeerID" -> senderPeerID,
    "receiverPeerID" -> receiverPeerID,
    "feedbackID" -> feedbackID,
    "queueLength" -> queueLength
  )

}
