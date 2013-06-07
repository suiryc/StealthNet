package stealthnet.scala.util

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

// scalastyle:off magic.number
@RunWith(classOf[JUnitRunner])
class HexDumperSuite extends FunSuite {

  /*
   * 0000: 1C CC 54 C6 88 85 37 88 - 8E 91 A8 19 0D 8B 56 8A | [..T...7.......V.]
   * 0010: D6 38 A8 17 A3 7E 62 A4 - 44 28 A2 76             | [.8...~b.D(.v    ]
   */
  private val binary: Array[Byte] =
    Hash("1CCC54C6888537888E91A8190D8B568AD638A817A37E62A44428A276").bytes

  test("< 16 bytes array") {
    val a = HexDumper.dump(binary.take(3)).toString
    val b = """0000: 1C CC 54                -                         | [..T             ]"""
    assert(a === b)
  }

  test("= 16 bytes array") {
    val a = HexDumper.dump(binary.take(16)).toString
    val b = """0000: 1C CC 54 C6 88 85 37 88 - 8E 91 A8 19 0D 8B 56 8A | [..T...7.......V.]"""
    assert(a === b)
  }

  test("> 16 bytes array") {
    val a = HexDumper.dump(binary).toString
    val b = """0000: 1C CC 54 C6 88 85 37 88 - 8E 91 A8 19 0D 8B 56 8A | [..T...7.......V.]
0010: D6 38 A8 17 A3 7E 62 A4 - 44 28 A2 76             | [.8...~b.D(.v    ]"""
    assert(a === b)
  }

  test("dump(length)") {
    val a = HexDumper.dump(binary, length = 3).toString
    val b = """0000: 1C CC 54                -                         | [..T             ]"""
    assert(a === b)
  }

  test("dump(offset, length)") {
    val a = HexDumper.dump(binary, offset = 1, length = 3).toString
    val b = """0000: CC 54 C6                -                         | [.T.             ]"""
    assert(a === b)
  }

  test("dump(result)") {
    var result = new StringBuilder("#")
    val a = HexDumper.dump(binary, result = result).toString
    val b = """#
0000: 1C CC 54 C6 88 85 37 88 - 8E 91 A8 19 0D 8B 56 8A | [..T...7.......V.]
0010: D6 38 A8 17 A3 7E 62 A4 - 44 28 A2 76             | [.8...~b.D(.v    ]"""
    assert(a === b)
  }

  test("undump") {
    for (input <- List(binary.take(3), binary.take(16), binary)) {
      val a = HexDumper.dump(input).toString
      val b = HexDumper.undump(a)
      assert(b === input)
    }
  }

}
// scalastyle:on magic.number
