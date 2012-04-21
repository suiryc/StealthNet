package stealthnet.scala.network.protocol.commands

import java.io.InputStream
import stealthnet.scala.cryptography.Hash
import stealthnet.scala.network.protocol.{Encryption, ProtocolStream}

/* XXX - same content as command 0x52 */
object Command62 extends CommandBuilder {

  val code: Byte = 0x62

  def argumentDefinitions = List(
    HashArgumentDefinition("commandId", 48),
    HashArgumentDefinition("senderPeerID", 48),
    HashArgumentDefinition("sourceSearchID", 48),
    HashArgumentDefinition("hashedFileHash", 64)
  )

  def read(input: InputStream): Command = {
    val arguments = readArguments(input)
    val commandId = arguments("commandId").asInstanceOf[Hash]
    val senderPeerID = arguments("senderPeerID").asInstanceOf[Hash]
    val sourceSearchID = arguments("sourceSearchID").asInstanceOf[Hash]
    val hashedFileHash = arguments("hashedFileHash").asInstanceOf[Hash]

    new Command62(commandId, senderPeerID, sourceSearchID, hashedFileHash)
  }

}

class Command62(
  /* 48-bytes */
  val commandId: Hash,
  /* 48-bytes */
  val senderPeerID: Hash,
  /* 48-bytes */
  val sourceSearchID: Hash,
  /* 64-bytes */
  val hashedFileHash: Hash
) extends Command
{

  val code = Command62.code

  val encryption = Encryption.Rijndael

  assert(commandId != null)
  assert(senderPeerID != null)
  assert(sourceSearchID != null)
  assert(hashedFileHash != null)

  def argumentDefinitions = Command62.argumentDefinitions

  def arguments = Map(
    "commandId" -> commandId,
    "senderPeerID" -> senderPeerID,
    "sourceSearchID" -> sourceSearchID,
    "hashedFileHash" -> hashedFileHash
  )

}
