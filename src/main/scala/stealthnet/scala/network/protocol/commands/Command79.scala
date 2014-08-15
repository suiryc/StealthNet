package stealthnet.scala.network.protocol.commands

import java.io.InputStream
import stealthnet.scala.Constants
import stealthnet.scala.network.protocol.{BitSize, Encryption}
import stealthnet.scala.util.Hash

object Command79 extends CommandBuilder {

  val code: Byte = 0x79

  def argumentDefinitions = List(
    HashArgumentDefinition("commandId", Constants.hashLength_48B),
    HashArgumentDefinition("senderPeerID", Constants.hashLength_48B),
    HashArgumentDefinition("receiverPeerID", Constants.hashLength_48B),
    HashArgumentDefinition("downloadID", Constants.hashLength_48B),
    IntegerArgumentDefinition("sector", BitSize.Int),
    ByteArrayArgumentDefinition("sectorData", BitSize.Short),
    HashArgumentDefinition("sectorHashCodeResult", Constants.hashLength_64B)
  )

  def read(input: InputStream): Command = {
    val arguments = readArguments(input)
    val commandId = arguments("commandId").asInstanceOf[Hash]
    val senderPeerID = arguments("senderPeerID").asInstanceOf[Hash]
    val receiverPeerID = arguments("receiverPeerID").asInstanceOf[Hash]
    val downloadID = arguments("downloadID").asInstanceOf[Hash]
    val sector = arguments("sector").asInstanceOf[Long]
    val sectorData = arguments("sectorData").asInstanceOf[Array[Byte]]
    val sectorHashCodeResult = arguments("sectorHashCodeResult").asInstanceOf[Hash]

    new Command79(commandId, senderPeerID, receiverPeerID, downloadID, sector,
      sectorData, sectorHashCodeResult)
  }

}

class Command79(
  /* 48-bytes */
  val commandId: Hash,
  /* 48-bytes */
  val senderPeerID: Hash,
  /* 48-bytes */
  val receiverPeerID: Hash,
  /* 48-bytes */
  val downloadID: Hash,
  /* unsigned int */
  val sector: Long,
  val sectorData: Array[Byte],
  /* 64-bytes */
  val sectorHashCodeResult: Hash
) extends Command
{

  val code = Command79.code

  val encryption = Encryption.Rijndael

  // scalastyle:off null
  assert(commandId != null)
  assert(senderPeerID != null)
  assert(receiverPeerID != null)
  assert(downloadID != null)
  assert(sectorData != null)
  assert(sectorHashCodeResult != null)
  // scalastyle:on null

  def argumentDefinitions = Command79.argumentDefinitions

  def arguments = Map(
    "commandId" -> commandId,
    "senderPeerID" -> senderPeerID,
    "receiverPeerID" -> receiverPeerID,
    "downloadID" -> downloadID,
    "sector" -> sector,
    "sectorData" -> sectorData,
    "sectorHashCodeResult" -> sectorHashCodeResult
  )

}
