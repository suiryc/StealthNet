package perso.stealthnet.network.protocol.commands

import java.io.InputStream
import perso.stealthnet.core.cryptography.Hash
import perso.stealthnet.network.protocol.{Encryption, ProtocolStream}

object Command22 extends CommandBuilder {

  val code: Byte = 0x22

  def read(input: InputStream): Command = {
    val commandId = ProtocolStream.readBytes(input, 48)
    val senderPeerID = ProtocolStream.readBytes(input, 48)
    val searchID = ProtocolStream.readBytes(input, 48)
    val searchPattern = ProtocolStream.readString(input)

    new Command22(commandId, senderPeerID, searchID,
      searchPattern)
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

  val code = SearchCommand.code

  val encryption = Encryption.Rijndael

  assert(commandId != null)
  assert(senderPeerID != null)
  assert(searchID != null)
  assert(searchPattern != null)

  def arguments() = List(
    "commandId" -> HashArgument(commandId, 48),
    "senderPeerID" -> HashArgument(senderPeerID, 48),
    "searchID" -> HashArgument(searchID, 48),
    "searchPattern" -> StringArgument(searchPattern)
  )

}
