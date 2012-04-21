package stealthnet.scala.network.protocol.commands

import java.io.InputStream
import stealthnet.scala.cryptography.Hash
import stealthnet.scala.network.protocol.{Encryption, ProtocolStream}

object Command22 extends CommandBuilder {

  val code: Byte = 0x22

  def argumentDefinitions = List(
    HashArgumentDefinition("commandId", 48),
    HashArgumentDefinition("senderPeerID", 48),
    HashArgumentDefinition("searchID", 48),
    StringArgumentDefinition("searchPattern")
  )

  def read(input: InputStream): Command = {
    val arguments = readArguments(input)
    val commandId = arguments("commandId").asInstanceOf[Hash]
    val senderPeerID = arguments("senderPeerID").asInstanceOf[Hash]
    val searchID = arguments("searchID").asInstanceOf[Hash]
    val searchPattern = arguments("searchPattern").asInstanceOf[String]

    new Command22(commandId, senderPeerID, searchID, searchPattern)
  }

}

class Command22(
  /* 48-bytes */
  val commandId: Hash,
  /* 48-bytes */
  val senderPeerID: Hash,
  /* 48-bytes */
  val searchID: Hash,
  val searchPattern: String
) extends Command
{

  val code = Command22.code

  val encryption = Encryption.Rijndael

  assert(commandId != null)
  assert(senderPeerID != null)
  assert(searchID != null)
  assert(searchPattern != null)

  def argumentDefinitions = Command22.argumentDefinitions

  def arguments = Map(
    "commandId" -> commandId,
    "senderPeerID" -> senderPeerID,
    "searchID" -> searchID,
    "searchPattern" -> searchPattern
  )

}
