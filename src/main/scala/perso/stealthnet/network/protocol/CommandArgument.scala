package perso.stealthnet.network.protocol

import java.io.OutputStream
import perso.stealthnet.core.cryptography.Hash
import perso.stealthnet.util.HexDumper

trait CommandArgument {

  def write(output: OutputStream): Int

}

class ByteArrayArgument(val array: Array[Byte], bitSize: Int) extends CommandArgument {

  assert((bitSize % 8) == 0)

  def write(output: OutputStream): Int =
    ProtocolStream.writeBytes(output, array, bitSize)

  override def toString(): String = "\n" + HexDumper.dump(array) + "\n"

}
