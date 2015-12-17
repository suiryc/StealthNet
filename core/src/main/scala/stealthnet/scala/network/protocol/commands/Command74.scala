package stealthnet.scala.network.protocol.commands

import java.io.InputStream
import stealthnet.scala.Constants
import stealthnet.scala.network.protocol.Encryption
import suiryc.scala.util.Hash

object Command74 extends CommandBuilder {

  val code: Byte = 0x74

  def argumentDefinitions = List(
    HashArgumentDefinition("commandId", Constants.hashLength_48B),
    HashArgumentDefinition("senderPeerID", Constants.hashLength_48B),
    HashArgumentDefinition("receiverPeerID", Constants.hashLength_48B),
    HashArgumentDefinition("downloadID", Constants.hashLength_48B),
    HashArgumentDefinition("hashedFileHash", Constants.hashLength_64B)
  )

  def read(input: InputStream): Command = {
    val arguments = readArguments(input)
    val commandId = arguments("commandId").asInstanceOf[Hash]
    val senderPeerID = arguments("senderPeerID").asInstanceOf[Hash]
    val receiverPeerID = arguments("receiverPeerID").asInstanceOf[Hash]
    val downloadID = arguments("downloadID").asInstanceOf[Hash]
    val hashedFileHash = arguments("hashedFileHash").asInstanceOf[Hash]

    new Command74(commandId, senderPeerID, receiverPeerID, downloadID,
      hashedFileHash)
  }

}

class Command74(
  /* 48-bytes */
  val commandId: Hash,
  /* 48-bytes */
  val senderPeerID: Hash,
  /* 48-bytes */
  val receiverPeerID: Hash,
  /* 48-bytes */
  val downloadID: Hash,
  /* 64-bytes */
  val hashedFileHash: Hash
) extends Command
{

  val code = Command74.code

  val encryption = Encryption.Rijndael

  // scalastyle:off null
  assert(commandId != null)
  assert(senderPeerID != null)
  assert(receiverPeerID != null)
  assert(downloadID != null)
  assert(hashedFileHash != null)
  // scalastyle:on null

  def argumentDefinitions = Command74.argumentDefinitions

  def arguments = Map(
    "commandId" -> commandId,
    "senderPeerID" -> senderPeerID,
    "receiverPeerID" -> receiverPeerID,
    "downloadID" -> downloadID,
    "hashedFileHash" -> hashedFileHash
  )

}
