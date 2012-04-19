package perso.stealthnet.network.protocol.commands

import java.io.InputStream
import perso.stealthnet.core.cryptography.Hash
import perso.stealthnet.network.protocol.{Encryption, ProtocolStream}

object Command52 extends CommandBuilder {

  val code: Byte = 0x52

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

    new Command52(commandId, senderPeerID, sourceSearchID, hashedFileHash)
  }

}

class Command52(
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

  val code = Command52.code

  val encryption = Encryption.Rijndael

  assert(commandId != null)
  assert(senderPeerID != null)
  assert(sourceSearchID != null)
  assert(hashedFileHash != null)

  def argumentDefinitions = Command52.argumentDefinitions

  def arguments = Map(
    "commandId" -> commandId,
    "senderPeerID" -> senderPeerID,
    "sourceSearchID" -> sourceSearchID,
    "hashedFileHash" -> hashedFileHash
  )

}
