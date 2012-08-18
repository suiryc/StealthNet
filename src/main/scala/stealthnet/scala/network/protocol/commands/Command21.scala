package stealthnet.scala.network.protocol.commands

import java.io.InputStream
import stealthnet.scala.Constants
import stealthnet.scala.cryptography.Hash
import stealthnet.scala.network.protocol.{BitSize, Encryption, ProtocolStream}

object Command21 extends CommandBuilder {

  val code: Byte = 0x21

  def argumentDefinitions = List(
    HashArgumentDefinition("commandId", Constants.hashLength_48B),
    IntegerArgumentDefinition("reserved", BitSize.Short),
    IntegerArgumentDefinition("hopCount", BitSize.Short),
    HashArgumentDefinition("senderPeerID", Constants.hashLength_48B),
    HashArgumentDefinition("searchID", Constants.hashLength_48B),
    StringArgumentDefinition("searchPattern")
  )

  def read(input: InputStream): Command = {
    val arguments = readArguments(input)
    val commandId = arguments("commandId").asInstanceOf[Hash]
    val reserved = arguments("reserved").asInstanceOf[Long].intValue
    val hopCount = arguments("hopCount").asInstanceOf[Long].intValue
    val senderPeerID = arguments("senderPeerID").asInstanceOf[Hash]
    val searchID = arguments("searchID").asInstanceOf[Hash]
    val searchPattern = arguments("searchPattern").asInstanceOf[String]

    new Command21(commandId, reserved, hopCount, senderPeerID, searchID,
      searchPattern)
  }

}

class Command21(
  /* 48-bytes */
  val commandId: Hash,
  /* unsigned short */
  val reserved: Int,
  /* unsigned short */
  val hopCount: Int,
  /* 48-bytes */
  val senderPeerID: Hash,
  /* 48-bytes */
  val searchID: Hash,
  val searchPattern: String
) extends Command
{

  val code = Command21.code

  val encryption = Encryption.Rijndael

  // scalastyle:off null
  assert(commandId != null)
  assert(senderPeerID != null)
  assert(searchID != null)
  assert(searchPattern != null)
  // scalastyle:on null

  def argumentDefinitions = Command21.argumentDefinitions

  def arguments = Map(
    "commandId" -> commandId,
    "reserved" -> reserved,
    "hopCount" -> hopCount,
    "senderPeerID" -> senderPeerID,
    "searchID" -> searchID,
    "searchPattern" -> searchPattern
  )

}
