package stealthnet.scala.network.protocol.commands

import java.io.InputStream
import stealthnet.scala.Constants
import stealthnet.scala.network.protocol.Encryption
import suiryc.scala.util.Hash

object Command7A extends CommandBuilder {

  val code: Byte = 0x7A

  def argumentDefinitions = List(
    HashArgumentDefinition("commandId", Constants.hashLength_48B),
    HashArgumentDefinition("senderPeerID", Constants.hashLength_48B),
    HashArgumentDefinition("receiverPeerID", Constants.hashLength_48B),
    HashArgumentDefinition("downloadID", Constants.hashLength_48B),
    HashArgumentDefinition("fileHashCodeResult", Constants.hashLength_64B)
  )

  def read(input: InputStream): Command = {
    val arguments = readArguments(input)
    val commandId = arguments("commandId").asInstanceOf[Hash]
    val senderPeerID = arguments("senderPeerID").asInstanceOf[Hash]
    val receiverPeerID = arguments("receiverPeerID").asInstanceOf[Hash]
    val downloadID = arguments("downloadID").asInstanceOf[Hash]
    val fileHashCodeResult = arguments("fileHashCodeResult").asInstanceOf[Hash]

    new Command7A(commandId, senderPeerID, receiverPeerID, downloadID,
      fileHashCodeResult)
  }

}

class Command7A(
  /* 48-bytes */
  val commandId: Hash,
  /* 48-bytes */
  val senderPeerID: Hash,
  /* 48-bytes */
  val receiverPeerID: Hash,
  /* 48-bytes */
  val downloadID: Hash,
  /* 64-bytes */
  val fileHashCodeResult: Hash
) extends Command
{

  val code = Command7A.code

  val encryption = Encryption.Rijndael

  // scalastyle:off null
  assert(commandId != null)
  assert(senderPeerID != null)
  assert(receiverPeerID != null)
  assert(downloadID != null)
  assert(fileHashCodeResult != null)
  // scalastyle:on null

  def argumentDefinitions = Command7A.argumentDefinitions

  def arguments = Map(
    "commandId" -> commandId,
    "senderPeerID" -> senderPeerID,
    "receiverPeerID" -> receiverPeerID,
    "downloadID" -> downloadID,
    "fileHashCodeResult" -> fileHashCodeResult
  )

}
