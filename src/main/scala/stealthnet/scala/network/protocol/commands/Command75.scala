package stealthnet.scala.network.protocol.commands

import java.io.InputStream
import stealthnet.scala.cryptography.Hash
import stealthnet.scala.network.protocol.{BitSize, Encryption, ProtocolStream}

object Command75 extends CommandBuilder {

  val code: Byte = 0x75

  def argumentDefinitions = List(
    HashArgumentDefinition("commandId", 48),
    HashArgumentDefinition("senderPeerID", 48),
    HashArgumentDefinition("receiverPeerID", 48),
    HashArgumentDefinition("downloadID", 48),
    IntegerArgumentDefinition("queuePosition", BitSize.Int)
  )

  def read(input: InputStream): Command = {
    val arguments = readArguments(input)
    val commandId = arguments("commandId").asInstanceOf[Hash]
    val senderPeerID = arguments("senderPeerID").asInstanceOf[Hash]
    val receiverPeerID = arguments("receiverPeerID").asInstanceOf[Hash]
    val downloadID = arguments("downloadID").asInstanceOf[Hash]
    val queuePosition = arguments("queuePosition").asInstanceOf[Long]

    new Command75(commandId, senderPeerID, receiverPeerID, downloadID,
      queuePosition)
  }

}

class Command75(
  /* 48-bytes */
  val commandId: Hash,
  /* 48-bytes */
  val senderPeerID: Hash,
  /* 48-bytes */
  val receiverPeerID: Hash,
  /* 48-bytes */
  val downloadID: Hash,
  /* unsigned int */
  val queuePosition: Long
) extends Command
{

  val code = Command75.code

  val encryption = Encryption.Rijndael

  assert(commandId != null)
  assert(senderPeerID != null)
  assert(receiverPeerID != null)
  assert(downloadID != null)

  def argumentDefinitions = Command75.argumentDefinitions

  def arguments = Map(
    "commandId" -> commandId,
    "senderPeerID" -> senderPeerID,
    "receiverPeerID" -> receiverPeerID,
    "downloadID" -> downloadID,
    "queuePosition" -> queuePosition
  )

}
