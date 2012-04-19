package perso.stealthnet.network.protocol.commands

import java.io.{InputStream, OutputStream}
import java.math.BigInteger
import perso.stealthnet.core.cryptography.Hash
import perso.stealthnet.network.protocol.{BitSize, ProtocolStream}
import perso.stealthnet.util.HexDumper

trait CommandArgument {

  def write(output: OutputStream): Int

}

trait CommandArgumentBuilder[T] {

  def read(input: InputStream): T

}

trait CommandArguments extends CommandArgument {

  def arguments(): List[(String, CommandArgument)]

  def write(output: OutputStream): Int = {
    var unencryptedLength: Int = 0
    for ((name, value) <- arguments)
      unencryptedLength += ProtocolStream.write(output, value)
    unencryptedLength
  }

  override def toString =
    getClass.getSimpleName + arguments.map(tuple => tuple._1 + "=" + tuple._2)

}

object ByteArgument {

  def apply(value: Byte) = new ByteArgument(value)

}

class ByteArgument(value: Byte) extends CommandArgument {

  def write(output: OutputStream): Int =
    ProtocolStream.writeByte(output, value)

  override def toString(): String = "%02X".format(value)

}

object ByteArrayArgument {

  def apply(value: Array[Byte], bitSize: Int) =
    new ByteArrayArgument(value, bitSize)

}

class ByteArrayArgument(value: Array[Byte], bitSize: Int)
  extends CommandArgument
{

  assert((bitSize % 8) == 0)

  def write(output: OutputStream): Int =
    ProtocolStream.writeBytes(output, value, bitSize)

  override def toString(): String = "\n" + HexDumper.dump(value) + "\n"

}

object IntegerArgument {

  def apply(value: Long, bitSize: Int) = new IntegerArgument(value, bitSize)

}

class IntegerArgument(value: Long, bitSize: Int) extends CommandArgument {
  
  assert((bitSize % 8) == 0)

  def write(output: OutputStream): Int =
    ProtocolStream.writeInteger(output, value, bitSize)

  override def toString(): String = value.toString()

}

object BigIntegerArgument {

  def apply(value: BigInt) = new BigIntegerArgument(value)

  def apply(value: BigInteger) = new BigIntegerArgument(new BigInt(value))

}

class BigIntegerArgument(value: BigInt) extends CommandArgument {

  def write(output: OutputStream): Int =
    ProtocolStream.writeBigInteger(output, value)

  override def toString(): String = value.toString()

}

object StringArgument {

  def apply(value: String) = new StringArgument(value)

}

class StringArgument(value: String) extends CommandArgument {
  
  def write(output: OutputStream): Int =
    ProtocolStream.writeString(output, value)

  override def toString(): String = value

}

object HashArgument {

  def apply(value: Hash, length: Int) = new HashArgument(value, length)

}

class HashArgument(value: Hash, length: Int) extends CommandArgument {

  assert(value != null)
  assert(value.bytes.length == length)

  def write(output: OutputStream): Int =
    ProtocolStream.write(output, value.bytes)

  override def toString(): String = value.toString()

}

object ListArgument {

  def apply(value: List[CommandArgument]) = new ListArgument(value)

}

class ListArgument(value: List[CommandArgument]) extends CommandArgument {

  assert(value != null)

  def write(output: OutputStream): Int = {
    var result: Int = ProtocolStream.writeInteger(output, value.length, BitSize.Short)

    for (argument <- value)
      result += argument.write(output)

    result
  }

  override def toString(): String = value.toString()

}

object StringMapArgument {

  def apply(value: Map[String, String]) = new StringMapArgument(value)

}

class StringMapArgument(value: Map[String, String]) extends CommandArgument {

  assert(value != null)

  def write(output: OutputStream): Int = {
    var result: Int = ProtocolStream.writeInteger(output, value.size, BitSize.Short)

    for ((key, value) <- value) {
      result += ProtocolStream.writeString(output, key)
      result += ProtocolStream.writeString(output, value)
    }

    result
  }

  override def toString(): String = value.toString()

}
