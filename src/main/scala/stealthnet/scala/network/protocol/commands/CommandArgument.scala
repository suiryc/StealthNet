package stealthnet.scala.network.protocol.commands

import java.io.{InputStream, OutputStream}
import scala.collection.mutable
import scala.language.existentials
import stealthnet.scala.network.protocol.{BitSize, ProtocolStream}
import stealthnet.scala.network.protocol.exceptions.ProtocolException
import stealthnet.scala.util.{Hash, HexDumper}

/**
 * Command argument definition.
 *
 * Used to automate command arguments handling.
 */
abstract class CommandArgumentDefinition(val name: String)

/** Byte argument definition. */
case class ByteArgumentDefinition(override val name: String)
  extends CommandArgumentDefinition(name)

/** Bytes array argument definition. */
case class ByteArrayArgumentDefinition(override val name: String, bitSize: Int)
  extends CommandArgumentDefinition(name)
{
  assert((bitSize % 8) == 0)
}

/** Integer argument definition. */
case class IntegerArgumentDefinition(override val name: String, bitSize: Int)
  extends CommandArgumentDefinition(name)
{
  assert((bitSize % 8) == 0)
}

/** Big integer argument definition. */
case class BigIntegerArgumentDefinition(override val name: String)
  extends CommandArgumentDefinition(name)

/** String argument definition. */
case class StringArgumentDefinition(override val name: String)
  extends CommandArgumentDefinition(name)

/** Hash argument definition. */
case class HashArgumentDefinition(override val name: String, length: Int)
  extends CommandArgumentDefinition(name)

/** List of arguments definition. */
case class ListArgumentsDefinition(override val name: String, builder: CommandArgumentsReader[_])
  extends CommandArgumentDefinition(name)

/** Map of strings argument definition. */
case class StringMapArgumentDefinition(override val name: String)
  extends CommandArgumentDefinition(name)

/**
 * Command argument definitions.
 */
trait CommandArgumentDefinitions {

  /** Gets the arguments definition of this command. */
  def argumentDefinitions: List[CommandArgumentDefinition]

}

/**
 * Command arguments reader.
 *
 * Used when reading and building received commands.
 */
trait CommandArgumentsReader[T] {

  this: CommandArgumentDefinitions =>

  /**
   * Reads arguments.
   *
   * Produces a new object representing the read command arguments.
   *
   * @param input stream to read from
   * @return object built from read arguments
   */
  def read(input: InputStream): T

  /**
   * Reads arguments.
   *
   * Sequentially reads this command arguments, determined through the
   * available list of arguments definitions.
   *
   * @param input stream to read from
   * @return map which associates each argument name to its read value
   */
  // scalastyle:off method.length
  def readArguments(input: InputStream): Map[String, Any] = {
    var result: mutable.Map[String, Any] = mutable.Map()

    var argumentName: String = ""
    try {
      for (definition <- argumentDefinitions) {
        argumentName = definition.name
        result += argumentName -> (definition match {
          case ByteArgumentDefinition(name) =>
            ProtocolStream.readByte(input)

          case ByteArrayArgumentDefinition(name, bitSize) =>
            ProtocolStream.readBytes(input, bitSize)

          case IntegerArgumentDefinition(name, bitSize) =>
            ProtocolStream.readInteger(input, bitSize)

          case BigIntegerArgumentDefinition(name) =>
            ProtocolStream.readBigInteger(input)

          case StringArgumentDefinition(name) =>
            ProtocolStream.readString(input)

          case HashArgumentDefinition(name, length) =>
            ProtocolStream.read(input, length):Hash

          case ListArgumentsDefinition(name, builder) =>
            var list: mutable.ListBuffer[Any] = mutable.ListBuffer()
            val size = ProtocolStream.readInteger(input, BitSize.Short).intValue
            for (i <- 0 until size) {
              list += builder.read(input)
            }
            /* no need to be immutable anymore */
            list.toList

          case StringMapArgumentDefinition(name) =>
            var map: mutable.Map[String, String] = mutable.Map()
            val size = ProtocolStream.readInteger(input, BitSize.Short).intValue
            for (i <- 0 until size) {
              val key = ProtocolStream.readString(input)
              val value = ProtocolStream.readString(input)
              map += key -> value
            }
            /* no need to be immutable anymore */
            map.toMap
        })
      }
    }
    catch {
      case e: Throwable =>
        throw new ProtocolException(s"Cannot read command argument[$argumentName]. Arguments that could be read: $result", e)
    }

    /* no need to be immutable anymore */
    result.toMap
  }
  // scalastyle:on method.length

}

/**
 * Command arguments.
 */
trait CommandArguments extends CommandArgumentDefinitions {

  /**
   * Gets the list of available arguments.
   *
   * @return a map associating each argument with its value
   */
  def arguments: Map[String, Any]

  /**
   * Writes this command arguments.
   *
   * Sequentially writes this command arguments, determined through the
   * available list of arguments definitions and the corresponding list of
   * arguments values.
   *
   * @param output stream to write to
   * @return number of written bytes
   */
  def write(output: OutputStream): Int = {
    var unencryptedLength: Int = 0

    for (definition <- argumentDefinitions) {
      val value = arguments(definition.name)
      unencryptedLength += (definition match {
        case ByteArgumentDefinition(name) =>
          ProtocolStream.writeByte(output, value.asInstanceOf[Byte])

        case ByteArrayArgumentDefinition(name, bitSize) =>
          ProtocolStream.writeBytes(output, value.asInstanceOf[Array[Byte]], bitSize)

        case IntegerArgumentDefinition(name, bitSize) =>
          ProtocolStream.writeInteger(output, value.asInstanceOf[Number].longValue, bitSize)

        case BigIntegerArgumentDefinition(name) =>
          ProtocolStream.writeBigInteger(output, value.asInstanceOf[BigInt])

        case StringArgumentDefinition(name) =>
          ProtocolStream.writeString(output, value.asInstanceOf[String])

        case HashArgumentDefinition(name, length) =>
          val hash = value.asInstanceOf[Hash]
          assert(hash.bytes.length == length)
          ProtocolStream.write(output, hash.bytes)

        case ListArgumentsDefinition(name, builder) =>
          val list = value.asInstanceOf[List[CommandArguments]]
          var result = ProtocolStream.writeInteger(output, list.length, BitSize.Short)
          for (argument <- list)
            result += argument.write(output)
          result

        case StringMapArgumentDefinition(name) =>
          val map = value.asInstanceOf[Map[String, String]]
          var result: Int = ProtocolStream.writeInteger(output, map.size, BitSize.Short)
          for ((key, value) <- map) {
            result += ProtocolStream.writeString(output, key)
            result += ProtocolStream.writeString(output, value)
          }
          result
      })
    }

    unencryptedLength
  }

  def argumentsToString = argumentDefinitions.map(definition => {
    val name = definition.name
    val value = arguments(name)
    name + '=' + (definition match {
      case ByteArgumentDefinition(name) =>
        "%02X".format(value)

      case ByteArrayArgumentDefinition(name, bitSize) =>
        "\n" + HexDumper.dump(value.asInstanceOf[Array[Byte]]) + "\n"

      case IntegerArgumentDefinition(name, bitSize) =>
        value.toString

      case BigIntegerArgumentDefinition(name) =>
        value.toString

      case StringArgumentDefinition(name) =>
        value.toString

      case HashArgumentDefinition(name, length) =>
        value.toString

      case ListArgumentsDefinition(name, builder) =>
        value.toString

      case StringMapArgumentDefinition(name) =>
        value.toString
    })
  }).mkString("(", ", ", ")")

  override def toString = getClass.getSimpleName + argumentsToString

}
