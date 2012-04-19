package perso.stealthnet.network.protocol.commands

import java.io.InputStream
import perso.stealthnet.core.cryptography.Hash
import perso.stealthnet.network.protocol.{BitSize, Encryption, ProtocolStream}

object Command21 extends CommandBuilder {

  val code: Byte = 0x21

  def read(input: InputStream): Command = {
    val commandId = ProtocolStream.readBytes(input, 48)
    val reserved = ProtocolStream.readInteger(input, BitSize.Short).intValue
    val hopCount = ProtocolStream.readInteger(input, BitSize.Short).intValue
    val senderPeerID = ProtocolStream.readBytes(input, 48)
    val searchID = ProtocolStream.readBytes(input, 48)
    val searchPattern = ProtocolStream.readString(input)

    new Command21(commandId, reserved, hopCount, senderPeerID, searchID,
      searchPattern)
  }

}

class Command21(
  /* 48-bytes */
  val commandId: Hash,
  val reserved: Int,
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

  assert(commandId != null)
  assert(senderPeerID != null)
  assert(searchID != null)
  assert(searchPattern != null)

  def arguments() = List(
    "commandId" -> HashArgument(commandId, 48),
    "reserved" -> IntegerArgument(reserved, BitSize.Short),
    "hopCount" -> IntegerArgument(hopCount, BitSize.Short),
    "senderPeerID" -> HashArgument(senderPeerID, 48),
    "searchID" -> HashArgument(searchID, 48),
    "searchPattern" -> StringArgument(searchPattern)
  )

}
