package stealthnet.scala.network.protocol

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, EOFException}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import scala.collection.mutable
import stealthnet.scala.Constants

// scalastyle:off magic.number
@RunWith(classOf[JUnitRunner])
class ProtocolStreamSuite extends FunSuite {

  private def buildArray(seq: Any*) = {
    val output = new ByteArrayOutputStream()
    for (v <- seq)
      v match {
        case v: Byte =>
          output.write(v.toInt)

        case v: Int =>
          output.write(v)

        case v: Array[Byte] =>
          output.write(v)

        case v: List[_] =>
          /* due to type erasure, we don't know the exact type of the list,
           * except it's a Number (may be a Byte, Int, ...) */
          output.write(v.map(_.asInstanceOf[Number].byteValue).toArray)
      }
    output.close()
    output.toByteArray
  }
  private def buildInput(seq: Any*) =
    new ByteArrayInputStream(buildArray(seq: _*))
  private def wrapArray(seq: Any*) =
    buildArray(seq: _*):mutable.WrappedArray[Byte]

  test("convertInteger") {
    val tests: List[((Long, Int), Array[Byte])] = List(
      ((0x00000000L, BitSize.Byte), buildArray(0x00)),
      ((0x00000001L, BitSize.Byte), buildArray(0x01)),
      ((0x0000007FL, BitSize.Byte), buildArray(0x7F)),
      ((0x00000080L, BitSize.Byte), buildArray(0x80)),
      ((0x000000FEL, BitSize.Byte), buildArray(0xFE)),
      ((0x000000FFL, BitSize.Byte), buildArray(0xFF)),
      ((0x00000000L, BitSize.Short), buildArray(0x00, 0x00)),
      ((0x00000001L, BitSize.Short), buildArray(0x01, 0x00)),
      ((0x00007FFFL, BitSize.Short), buildArray(0xFF, 0x7F)),
      ((0x00008000L, BitSize.Short), buildArray(0x00, 0x80)),
      ((0x0000FFFEL, BitSize.Short), buildArray(0xFE, 0xFF)),
      ((0x0000FFFFL, BitSize.Short), buildArray(0xFF, 0xFF)),
      ((0x00000000L, BitSize.Int), buildArray(0x00, 0x00, 0x00, 0x00)),
      ((0x00000001L, BitSize.Int), buildArray(0x01, 0x00, 0x00, 0x00)),
      ((0x7FFFFFFFL, BitSize.Int), buildArray(0xFF, 0xFF, 0xFF, 0x7F)),
      ((0x80000000L, BitSize.Int), buildArray(0x00, 0x00, 0x00, 0x80)),
      ((0xFFFFFFFEL, BitSize.Int), buildArray(0xFE, 0xFF, 0xFF, 0xFF)),
      ((0xFFFFFFFFL, BitSize.Int), buildArray(0xFF, 0xFF, 0xFF, 0xFF))
    )

    for (((value, bitSize), bytes) <- tests) {
      assert(wrapArray(bytes)
        === wrapArray(ProtocolStream.convertInteger(value, bitSize)))
    }

    /* special tests */
    intercept[IllegalArgumentException] {
      ProtocolStream.convertInteger(0x0100L, BitSize.Byte)
    }

    intercept[IllegalArgumentException] {
      ProtocolStream.convertInteger(0x010000L, BitSize.Short)
    }

    intercept[IllegalArgumentException] {
      ProtocolStream.convertInteger(0x0100000000L, BitSize.Int)
    }
  }

  test("writeHeader") {
    val output = new ByteArrayOutputStream()
    ProtocolStream.writeHeader(output)
    assert(wrapArray(Constants.protocolRAW) === wrapArray(output.toByteArray))
  }

  test("readByte+writeByte") {
    val tests: List[Byte] = List(
      0x00.asInstanceOf[Byte],
      0x01.asInstanceOf[Byte],
      0x7F.asInstanceOf[Byte],
      0x80.asInstanceOf[Byte],
      0xFE.asInstanceOf[Byte],
      0xFF.asInstanceOf[Byte]
    )

    for (value <- tests) {
      /* test 'read' */
      assert(value === ProtocolStream.readByte(buildInput(value)))
      /* test 'write' */
      val output = new ByteArrayOutputStream()
      assert(1 === ProtocolStream.writeByte(output, value))
      output.close()
      assert(wrapArray(value) === wrapArray(output.toByteArray))
    }

    /* special tests */
    val input = buildInput(0x01)
    ProtocolStream.readByte(input)
    intercept[EOFException] {
      ProtocolStream.readByte(input)
    }
  }

  test("readBytes+writeBytes") {
    val list1 = for (i <- (0x01 to 0xFF).toList) yield i
    val list2 = for (i <- (0x00 to 0xFF).toList) yield i
    val list3 = for (i <- (0x0001 to 0xFFFF).toList) yield i
    val list4 = for (i <- (0x0000 to 0xFFFF).toList) yield i
    val tests: List[(Array[Byte], Int)] = List(
      (buildArray(), BitSize.Byte),
      (buildArray(0x00), BitSize.Byte),
      (buildArray(list1), BitSize.Byte),
      (buildArray(), BitSize.Short),
      (buildArray(0xFF), BitSize.Short),
      (buildArray(list3), BitSize.Short),
      (buildArray(), BitSize.Int),
      (buildArray(0xFF), BitSize.Int),
      (buildArray(list4), BitSize.Int)
    )

    for ((value, bitSize) <- tests) {
      val bytes = buildArray(ProtocolStream.convertInteger(value.length.toLong, bitSize), value)
      /* test 'read' */
      assert(wrapArray(value)
        === wrapArray(ProtocolStream.readBytes(buildInput(bytes), bitSize)))
      /* test 'write' */
      val output = new ByteArrayOutputStream()
      assert((value.length + bitSize / 8)
        === ProtocolStream.writeBytes(output, value, bitSize))
      output.close()
      assert(wrapArray(bytes) === wrapArray(output.toByteArray))
    }

    /* special tests */
    intercept[EOFException] {
      ProtocolStream.readBytes(buildInput(), BitSize.Byte)
    }
    intercept[EOFException] {
      ProtocolStream.readBytes(buildInput(0x02, 0x00), BitSize.Byte)
    }
    intercept[IllegalArgumentException] {
      ProtocolStream.writeBytes(new ByteArrayOutputStream(), buildArray(list2), BitSize.Byte)
    }

    intercept[EOFException] {
      ProtocolStream.readBytes(buildInput(), BitSize.Short)
    }
    intercept[EOFException] {
      ProtocolStream.readBytes(buildInput(0x00), BitSize.Short)
    }
    intercept[EOFException] {
      ProtocolStream.readBytes(buildInput(ProtocolStream.convertInteger(2, BitSize.Short), 0x00), BitSize.Short)
    }
    intercept[IllegalArgumentException] {
      ProtocolStream.writeBytes(new ByteArrayOutputStream(), buildArray(list4), BitSize.Short)
    }

    intercept[EOFException] {
      ProtocolStream.readBytes(buildInput(), BitSize.Int)
    }
    intercept[EOFException] {
      ProtocolStream.readBytes(buildInput(0x00, 0x00, 0x00), BitSize.Int)
    }
    intercept[EOFException] {
      ProtocolStream.readBytes(buildInput(ProtocolStream.convertInteger(2, BitSize.Int), 0x00), BitSize.Int)
    }
    /* not going to build a 2^32 array :) */
  }

  test("readInteger+writeInteger") {
    val tests: List[(Long, Int)] = List(
      (0x00000000L, BitSize.Byte),
      (0x00000001L, BitSize.Byte),
      (0x0000007FL, BitSize.Byte),
      (0x00000080L, BitSize.Byte),
      (0x000000FEL, BitSize.Byte),
      (0x000000FFL, BitSize.Byte),
      (0x00000000L, BitSize.Short),
      (0x00000001L, BitSize.Short),
      (0x00007FFFL, BitSize.Short),
      (0x00008000L, BitSize.Short),
      (0x0000FFFEL, BitSize.Short),
      (0x0000FFFFL, BitSize.Short),
      (0x00000000L, BitSize.Int),
      (0x00000001L, BitSize.Int),
      (0x7FFFFFFFL, BitSize.Int),
      (0x80000000L, BitSize.Int),
      (0xFFFFFFFEL, BitSize.Int),
      (0xFFFFFFFFL, BitSize.Int)
    )

    for ((value, bitSize) <- tests) {
      val bytes = ProtocolStream.convertInteger(value, bitSize)
      /* test 'read' */
      assert(value === ProtocolStream.readInteger(buildInput(bytes), bitSize))
      /* test 'write' */
      val output = new ByteArrayOutputStream()
      assert((bitSize / 8) === ProtocolStream.writeInteger(output, value, bitSize))
      output.close()
      assert(wrapArray(bytes) === wrapArray(output.toByteArray))
    }

    /* special tests */
    intercept[EOFException] {
      ProtocolStream.readInteger(buildInput(), BitSize.Byte)
    }
    intercept[IllegalArgumentException] {
      ProtocolStream.writeInteger(new ByteArrayOutputStream(), 0x0100L, BitSize.Byte)
    }

    intercept[EOFException] {
      ProtocolStream.readInteger(buildInput(0x00), BitSize.Short)
    }
    intercept[IllegalArgumentException] {
      ProtocolStream.writeInteger(new ByteArrayOutputStream(), 0x010000L, BitSize.Short)
    }

    intercept[EOFException] {
      ProtocolStream.readInteger(buildInput(0x00, 0x00, 0x00), BitSize.Int)
    }
    intercept[IllegalArgumentException] {
      ProtocolStream.writeInteger(new ByteArrayOutputStream(), 0x0100000000L, BitSize.Int)
    }
  }

  test("readBigInteger+writeBigInteger") {
    val tests: List[(BigInt, Array[Byte])] = List(
      BigInt(0)
        -> buildArray(0x01, 0x00, 0x00),
      BigInt(1)
        -> buildArray(0x01, 0x00, 0x01),
      BigInt(255)
        -> buildArray(0x01, 0x00, 0xFF),
      ((BigInt(1) << 64) - 2)
        -> buildArray(0x08, 0x00, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFE),
      (BigInt(1) << 64)
        -> buildArray(0x09, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
    )

    for ((value, bytes) <- tests) {
      /* test 'read' */
      assert(value === ProtocolStream.readBigInteger(buildInput(bytes)))
      /* test 'write' */
      val output = new ByteArrayOutputStream()
      assert(bytes.length === ProtocolStream.writeBigInteger(output, value))
      output.close()
      assert(wrapArray(bytes) === wrapArray(output.toByteArray))
    }

    /* special tests */
    assert(BigInt(255) ===
      ProtocolStream.readBigInteger(buildInput(0x02, 0x00, 0x00, 0xFF)))
    intercept[EOFException] {
      ProtocolStream.readBigInteger(buildInput(0x02, 0x00, 0x00))
    }
  }

  test("readString+writeString") {
    val tests: List[String] = List(
      "",
      "The quick brown fox jumps over the lazy dog",
      "\u2200\u2208\u2203"
    )

    for (value <- tests) {
      val utf8 = value.getBytes("UTF-8")
      val bytes = buildArray(ProtocolStream.convertInteger(utf8.length.toLong, BitSize.Short), utf8)
      /* test 'read' */
      assert(value === ProtocolStream.readString(buildInput(bytes)))
      /* test 'write' */
      val output = new ByteArrayOutputStream()
      assert(bytes.length === ProtocolStream.writeString(output, value))
      output.close()
      assert(wrapArray(bytes) === wrapArray(output.toByteArray))
    }
  }

  test("read+write") {
    val tests: List[Array[Byte]] = List(
      buildArray(),
      buildArray(0x00),
      buildArray(0x01, 0x02, 0x03, 0x04, 0x05)
    )

    for (value <- tests) {
      /* test 'read' */
      assert(wrapArray(value) ===
        wrapArray(ProtocolStream.read(buildInput(value), value.length)))
      /* test 'write' */
      val output = new ByteArrayOutputStream()
      assert(value.length === ProtocolStream.write(output, value))
      output.close()
      assert(wrapArray(value) === wrapArray(output.toByteArray))
    }
  }

}
// scalastyle:on magic.number
