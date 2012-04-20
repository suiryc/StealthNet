package perso.stealthnet.network.protocol.commands

import java.io.{InputStream, OutputStream}
import java.math.BigInteger
import scala.collection.mutable
import perso.stealthnet.cryptography.Hash
import perso.stealthnet.network.protocol.{BitSize, ProtocolStream}
import perso.stealthnet.util.HexDumper
import perso.stealthnet.util.Logging
import perso.stealthnet.util.EmptyLoggingContext

trait CommandArgument {

  def write(output: OutputStream): Int

}

abstract class CommandArgumentDefinition(val name: String)

case class ByteArgumentDefinition(override val name: String)
  extends CommandArgumentDefinition(name)

case class ByteArrayArgumentDefinition(override val name: String, bitSize: Int)
  extends CommandArgumentDefinition(name)
{
  assert((bitSize % 8) == 0)
}

case class IntegerArgumentDefinition(override val name: String, bitSize: Int)
  extends CommandArgumentDefinition(name)
{
  assert((bitSize % 8) == 0)
}

case class BigIntegerArgumentDefinition(override val name: String)
  extends CommandArgumentDefinition(name)

case class StringArgumentDefinition(override val name: String)
  extends CommandArgumentDefinition(name)

case class HashArgumentDefinition(override val name: String, length: Int)
  extends CommandArgumentDefinition(name)

case class ListArgumentDefinition(override val name: String, builder: CommandArgumentBuilder[_])
  extends CommandArgumentDefinition(name)

case class StringMapArgumentDefinition(override val name: String)
  extends CommandArgumentDefinition(name)

trait CommandArgumentDefinitions {

  def argumentDefinitions: List[CommandArgumentDefinition]

}

trait CommandArgumentBuilder[T] extends Logging with EmptyLoggingContext {

  this: CommandArgumentDefinitions =>

  def read(input: InputStream): T

  def readArguments(input: InputStream): Map[String, Any] = {
    var result: mutable.Map[String, Any] = mutable.Map()

    try {
      for (definition <- argumentDefinitions) {
        result += definition.name -> (definition match {
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

          case ListArgumentDefinition(name, builder) =>
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
      /* XXX - wrap e (which is throwable) into another error which will be logged
       * by caller ? (may be cleaner than trying to log here ...) If so, remove
       * Logging trait etc */
      case e =>
        logger error("Cannot read command arguments. Currently known: " + result)
        /* propagate issue */
        throw e
    }

    /* no need to be immutable anymore */
    result.toMap
  }

}

trait CommandArguments extends CommandArgument with CommandArgumentDefinitions {

  def arguments: Map[String, Any]

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

        case ListArgumentDefinition(name, builder) =>
          val list = value.asInstanceOf[List[CommandArgument]]
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

  override def toString = getClass.getSimpleName + argumentDefinitions.map(definition => {
    val name = definition.name
    val value = arguments(name)
    name + '=' + (definition match {
      case ByteArgumentDefinition(name) =>
        "%02X".format(value)

      case ByteArrayArgumentDefinition(name, bitSize) =>
        "\n" + HexDumper.dump(value.asInstanceOf[Array[Byte]]) + "\n"

      case IntegerArgumentDefinition(name, bitSize) =>
        value.toString()

      case BigIntegerArgumentDefinition(name) =>
        value.toString()

      case StringArgumentDefinition(name) =>
        value.toString()

      case HashArgumentDefinition(name, length) =>
        value.toString()

      case ListArgumentDefinition(name, builder) =>
        value.toString()

      case StringMapArgumentDefinition(name) =>
        value.toString()
    })
  }).mkString("(", ", ", ")")

}
