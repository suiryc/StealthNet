package stealthnet.scala.network.protocol.commands

import java.io.InputStream
import stealthnet.scala.cryptography.Hash
import stealthnet.scala.network.protocol.{Encryption, ProtocolStream}

/* XXX - same content as command 0x50 */
object Command60 extends CommandBuilder {

  val code: Byte = 0x60

  def argumentDefinitions = List(
    HashArgumentDefinition("commandId", 48),
    HashArgumentDefinition("floodingHash", 48),
    HashArgumentDefinition("senderPeerID", 48),
    HashArgumentDefinition("sourceSearchID", 48),
    HashArgumentDefinition("hashedFileHash", 64)
  )

  def read(input: InputStream): Command = {
    val arguments = readArguments(input)
    val commandId = arguments("commandId").asInstanceOf[Hash]
    val floodingHash = arguments("floodingHash").asInstanceOf[Hash]
    val senderPeerID = arguments("senderPeerID").asInstanceOf[Hash]
    val sourceSearchID = arguments("sourceSearchID").asInstanceOf[Hash]
    val hashedFileHash = arguments("hashedFileHash").asInstanceOf[Hash]

    new Command60(commandId, floodingHash, senderPeerID, sourceSearchID,
      hashedFileHash)
  }

}

class Command60(
  /* 48-bytes */
  val commandId: Hash,
  /* 48-bytes */
  val floodingHash: Hash,
  /* 48-bytes */
  val senderPeerID: Hash,
  /* 48-bytes */
  val sourceSearchID: Hash,
  /* 64-bytes */
  val hashedFileHash: Hash
) extends Command
{

  val code = Command60.code

  val encryption = Encryption.Rijndael

  assert(commandId != null)
  assert(floodingHash != null)
  assert(senderPeerID != null)
  assert(sourceSearchID != null)
  assert(hashedFileHash != null)

  def argumentDefinitions = Command60.argumentDefinitions

  def arguments = Map(
    "commandId" -> commandId,
    "floodingHash" -> floodingHash,
    "senderPeerID" -> senderPeerID,
    "sourceSearchID" -> sourceSearchID,
    "hashedFileHash" -> hashedFileHash
  )

}
