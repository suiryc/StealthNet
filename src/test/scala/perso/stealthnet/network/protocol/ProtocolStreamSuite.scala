package perso.stealthnet.network.protocol

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, EOFException}
import scala.collection.mutable.WrappedArray
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ProtocolStreamSuite extends FunSuite {

  private def buildArray(seq: Any*) = {
    val output = new ByteArrayOutputStream()
    for (v <- seq)
      v match {
        case v: Byte =>
          output.write(v)

        case v: Int =>
          output.write(v)

        case v: Array[Byte] =>
          output.write(v)

        case v: List[Number] =>
          /* due to type erasure, we may have a List of something else (e.g. Integers) */
          output.write(v.map(_.byteValue).toArray)
      }
    output.close()
    output.toByteArray()
  }
  private def buildInput(seq: Any*) =
    new ByteArrayInputStream(buildArray(seq: _*))
  private def wrapArray(seq: Any*) =
    buildArray(seq: _*):WrappedArray[Byte]

  test("convertInteger") {
    assert(wrapArray(0x00)
      === wrapArray(ProtocolStream.convertInteger(0x00, BitSize.Byte)))
    assert(wrapArray(0x01)
      === wrapArray(ProtocolStream.convertInteger(0x01, BitSize.Byte)))
    assert(wrapArray(0x7F)
      === wrapArray(ProtocolStream.convertInteger(0x7F, BitSize.Byte)))
    assert(wrapArray(0x80)
      === wrapArray(ProtocolStream.convertInteger(0x80, BitSize.Byte)))
    assert(wrapArray(0xFE)
      === wrapArray(ProtocolStream.convertInteger(0xFE, BitSize.Byte)))
    assert(wrapArray(0xFF)
      === wrapArray(ProtocolStream.convertInteger(0xFF, BitSize.Byte)))
    intercept[IllegalArgumentException] {
      ProtocolStream.convertInteger(0x0100, BitSize.Byte)
    }

    assert(wrapArray(0x00, 0x00)
      === wrapArray(ProtocolStream.convertInteger(0x0000, BitSize.Short)))
    assert(wrapArray(0x01, 0x00)
      === wrapArray(ProtocolStream.convertInteger(0x0001, BitSize.Short)))
    assert(wrapArray(0x01, 0x02)
      === wrapArray(ProtocolStream.convertInteger(0x0201, BitSize.Short)))
    assert(wrapArray(0xFF, 0x7F)
      === wrapArray(ProtocolStream.convertInteger(0x7FFF, BitSize.Short)))
    assert(wrapArray(0x00, 0x80)
      === wrapArray(ProtocolStream.convertInteger(0x8000, BitSize.Short)))
    assert(wrapArray(0xFE, 0xFF)
      === wrapArray(ProtocolStream.convertInteger(0xFFFE, BitSize.Short)))
    assert(wrapArray(0xFF, 0xFF)
      === wrapArray(ProtocolStream.convertInteger(0xFFFF, BitSize.Short)))
    intercept[IllegalArgumentException] {
      ProtocolStream.convertInteger(0x010000, BitSize.Short)
    }
  }

  test("writeHeader") {
    val output = new ByteArrayOutputStream()
    ProtocolStream.writeHeader(output)
    assert(wrapArray(Constants.protocolRAW) === wrapArray(output.toByteArray()))
  }

  test("readByte") {
    assert(0x00.asInstanceOf[Byte] === ProtocolStream.readByte(buildInput(0x00)))
    assert(0x01.asInstanceOf[Byte] === ProtocolStream.readByte(buildInput(0x01)))
    assert(0x7F.asInstanceOf[Byte] === ProtocolStream.readByte(buildInput(0x7F)))
    assert(0x80.asInstanceOf[Byte]
      === ProtocolStream.readByte(buildInput(0x80)))
    assert(0xFE.asInstanceOf[Byte]
      === ProtocolStream.readByte(buildInput(0xFE)))
    assert(0xFF.asInstanceOf[Byte]
      === ProtocolStream.readByte(buildInput(0xFF)))
    val input = buildInput(0x01)
    ProtocolStream.readByte(input)
    intercept[EOFException] {
      ProtocolStream.readByte(input)
    }
  }

  private def _testWriteByte(v: Byte) {
    val output = new ByteArrayOutputStream()
    assert(1 === ProtocolStream.writeByte(output, v))
    output.close()
    assert(wrapArray(v) === wrapArray(output.toByteArray()))
  }

  test("writeByte") {
    _testWriteByte(0x00.asInstanceOf[Byte])
    _testWriteByte(0x01.asInstanceOf[Byte])
    _testWriteByte(0x7F.asInstanceOf[Byte])
    _testWriteByte(0x80.asInstanceOf[Byte])
    _testWriteByte(0xFE.asInstanceOf[Byte])
    _testWriteByte(0xFF.asInstanceOf[Byte])
  }

  test("readBytes") {
    assert(wrapArray()
      === wrapArray(ProtocolStream.readBytes(buildInput(0x00), BitSize.Byte)))
    assert(wrapArray(0x00)
      === wrapArray(ProtocolStream.readBytes(buildInput(0x01, 0x00), BitSize.Byte)))
    val list1 = for (i <- (0x01 to 0xFF).toList) yield i
    assert(wrapArray(list1)
      === wrapArray(ProtocolStream.readBytes(buildInput(list1.length, list1), BitSize.Byte)))
    intercept[EOFException] {
      ProtocolStream.readBytes(buildInput(), BitSize.Byte)
    }
    intercept[EOFException] {
      ProtocolStream.readBytes(buildInput(0x02, 0x00), BitSize.Byte)
    }

    assert(wrapArray()
      === wrapArray(ProtocolStream.readBytes(buildInput(ProtocolStream.convertInteger(0, BitSize.Short)), BitSize.Short)))
    assert(wrapArray(0xFF)
      === wrapArray(ProtocolStream.readBytes(buildInput(ProtocolStream.convertInteger(1, BitSize.Short), 0xFF), BitSize.Short)))
    val list2 = for (i <- (0x0001 to 0xFFFF).toList) yield i
    assert(wrapArray(list2)
      === wrapArray(ProtocolStream.readBytes(buildInput(
        ProtocolStream.convertInteger(list2.length, BitSize.Short), list2), BitSize.Short)))
    intercept[EOFException] {
      ProtocolStream.readBytes(buildInput(), BitSize.Short)
    }
    intercept[EOFException] {
      ProtocolStream.readBytes(buildInput(0x00), BitSize.Short)
    }
    intercept[EOFException] {
      ProtocolStream.readBytes(buildInput(ProtocolStream.convertInteger(2, BitSize.Short), 0x00), BitSize.Short)
    }
  }

  private def _testWriteBytes(v: Array[Byte], bitSize: Int) {
    val output = new ByteArrayOutputStream()
    assert((v.length + bitSize / 8) === ProtocolStream.writeBytes(output, v, bitSize))
    output.close()
    assert(wrapArray(ProtocolStream.convertInteger(v.length, bitSize), v) === wrapArray(output.toByteArray()))
  }

  test("writeBytes") {
    _testWriteBytes(buildArray(), BitSize.Byte)
    _testWriteBytes(buildArray(0x00), BitSize.Byte)
    val list1 = for (i <- (0x01 to 0xFF).toList) yield i
    _testWriteBytes(buildArray(list1), BitSize.Byte)
    val list2 = for (i <- (0x00 to 0xFF).toList) yield i
    intercept[IllegalArgumentException] {
      _testWriteBytes(buildArray(list2), BitSize.Byte)
    }

    _testWriteBytes(buildArray(), BitSize.Short)
    _testWriteBytes(buildArray(0x00), BitSize.Short)
    _testWriteBytes(buildArray(list1), BitSize.Short)
    _testWriteBytes(buildArray(list2), BitSize.Short)
    val list3 = for (i <- (0x0001 to 0xFFFF).toList) yield i
    _testWriteBytes(buildArray(list3), BitSize.Short)
    val list4 = for (i <- (0x0000 to 0xFFFF).toList) yield i
    intercept[IllegalArgumentException] {
      _testWriteBytes(buildArray(list4), BitSize.Short)
    }
  }

  test("readInteger") {
    assert(0x0000 === ProtocolStream.readInteger(buildInput(0x00), BitSize.Byte))
    assert(0x007F === ProtocolStream.readInteger(buildInput(0x7F), BitSize.Byte))
    assert(0x0080 === ProtocolStream.readInteger(buildInput(0x80), BitSize.Byte))
    assert(0x00FF === ProtocolStream.readInteger(buildInput(0xFF), BitSize.Byte))

    assert(0x0000 === ProtocolStream.readInteger(buildInput(0x00, 0x00), BitSize.Short))
    assert(0x0001 === ProtocolStream.readInteger(buildInput(0x01, 0x00), BitSize.Short))
    assert(0x7FFF === ProtocolStream.readInteger(buildInput(0xFF, 0x7F), BitSize.Short))
    assert(0x8000 === ProtocolStream.readInteger(buildInput(0x00, 0x80), BitSize.Short))
    assert(0xFFFE === ProtocolStream.readInteger(buildInput(0xFE, 0xFF), BitSize.Short))
    assert(0xFFFF === ProtocolStream.readInteger(buildInput(0xFF, 0xFF), BitSize.Short))
    intercept[EOFException] {
      assert(0x0000 === ProtocolStream.readInteger(buildInput(0x00), BitSize.Short))
    }
  }

  private def _testWriteInteger(v: Long, bitSize: Int) {
    val output = new ByteArrayOutputStream()
    assert((bitSize / 8) === ProtocolStream.writeInteger(output, v, bitSize))
    output.close()
    assert(wrapArray(ProtocolStream.convertInteger(v, bitSize)) === wrapArray(output.toByteArray()))
  }

  test("writeInteger") {
    _testWriteInteger(0x00, BitSize.Byte)
    _testWriteInteger(0x01, BitSize.Byte)
    _testWriteInteger(0x7F, BitSize.Byte)
    _testWriteInteger(0x80, BitSize.Byte)
    _testWriteInteger(0xFE, BitSize.Byte)
    _testWriteInteger(0xFF, BitSize.Byte)
    intercept[IllegalArgumentException] {
      _testWriteInteger(0x0100, BitSize.Byte)
    }

    _testWriteInteger(0x0000, BitSize.Short)
    _testWriteInteger(0x0001, BitSize.Short)
    _testWriteInteger(0x7FFF, BitSize.Short)
    _testWriteInteger(0x8000, BitSize.Short)
    _testWriteInteger(0xFFFE, BitSize.Short)
    _testWriteInteger(0xFFFF, BitSize.Short)
    intercept[IllegalArgumentException] {
      _testWriteInteger(0x010000, BitSize.Short)
    }
  }

  /* XXX - readBigInteger */
  /* XXX - writeBigInteger */
  /* XXX - readString */
  /* XXX - writeString */
  /* XXX - readHash */
  /* XXX - writeHash */
  /* XXX - read */

}
