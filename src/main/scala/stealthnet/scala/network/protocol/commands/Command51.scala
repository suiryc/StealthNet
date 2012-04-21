package stealthnet.scala.network.protocol.commands

import java.io.InputStream
import stealthnet.scala.cryptography.Hash
import stealthnet.scala.network.protocol.{BitSize, Encryption, ProtocolStream}

object Command51 extends CommandBuilder {

  val code: Byte = 0x51

  def argumentDefinitions = List(
    HashArgumentDefinition("commandId", 48),
    IntegerArgumentDefinition("reserved", BitSize.Short),
    IntegerArgumentDefinition("hopCount", BitSize.Short),
    HashArgumentDefinition("senderPeerID", 48),
    HashArgumentDefinition("sourceSearchID", 48),
    HashArgumentDefinition("hashedFileHash", 64)
  )

  def read(input: InputStream): Command = {
    val arguments = readArguments(input)
    val commandId = arguments("commandId").asInstanceOf[Hash]
    val reserved = arguments("reserved").asInstanceOf[Long].intValue
    val hopCount = arguments("hopCount").asInstanceOf[Long].intValue
    val senderPeerID = arguments("senderPeerID").asInstanceOf[Hash]
    val sourceSearchID = arguments("sourceSearchID").asInstanceOf[Hash]
    val hashedFileHash = arguments("hashedFileHash").asInstanceOf[Hash]

    new Command51(commandId, reserved, hopCount, senderPeerID, sourceSearchID,
      hashedFileHash)
  }

}

class Command51(
  /* 48-bytes */
  val commandId: Hash,
  /* unsigned short */
  val reserved: Int,
  /* unsigned short */
  val hopCount: Int,
  /* 48-bytes */
  val senderPeerID: Hash,
  /* 48-bytes */
  val sourceSearchID: Hash,
  /* 64-bytes */
  val hashedFileHash: Hash
) extends Command
{

  val code = Command51.code

  val encryption = Encryption.Rijndael

  assert(commandId != null)
  assert(senderPeerID != null)
  assert(sourceSearchID != null)
  assert(hashedFileHash != null)

  def argumentDefinitions = Command51.argumentDefinitions

  def arguments = Map(
    "commandId" -> commandId,
    "reserved" -> reserved,
    "hopCount" -> hopCount,
    "senderPeerID" -> senderPeerID,
    "sourceSearchID" -> sourceSearchID,
    "hashedFileHash" -> hashedFileHash
  )

}
