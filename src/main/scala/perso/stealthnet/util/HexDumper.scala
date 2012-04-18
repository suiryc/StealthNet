package perso.stealthnet.util

import scala.collection.mutable.IndexedSeqView
import perso.stealthnet.core.cryptography.Hash

object HexDumper {

  def dump(result: StringBuilder, data: Array[Byte], offset: Int, length: Int) {

    /* Let's have some fun with Stream */
    def stream: Stream[IndexedSeqView[Byte, Array[Byte]]] = {
      def loop(rest: IndexedSeqView[Byte, Array[Byte]]): Stream[IndexedSeqView[Byte, Array[Byte]]] = rest.splitAt(16) match {
        case (head, tail) if (!head.isEmpty) =>
          head #:: loop(tail)

        case _ =>
          Stream.empty
      }
      loop(data.view(offset, length))
    }

    def ensureLength(str: String, len: Int) =
      if (str.length >= len)
        str
      else
        str + " " * (len - str.length)

    def processHexSection(section: Array[Byte]) =
      section.map("%02X".format(_)).mkString(" ")

    def processAsciiSection(section: Array[Byte]) =
      section.map(c => if ((c >= 0x20) && (c < 0x7F)) c.asInstanceOf[Char] else '.').mkString("")

    def processLine(result: StringBuilder, line: IndexedSeqView[Byte, Array[Byte]], index: Int) {
      val (first, second) = line.force.splitAt(8)

      if (result.length > 0)
        result.append('\n')

        result.append("%04X: ".format(index))
            .append(ensureLength(processHexSection(first), 23))
            .append(" - ")
            .append(ensureLength(processHexSection(second), 23))
            .append(" | [")
            .append(ensureLength(processAsciiSection(first) + processAsciiSection(second), 16))
            .append(']')
    }

    @annotation.tailrec
    def processLines(result: StringBuilder, stream: Stream[IndexedSeqView[Byte, Array[Byte]]], index: Int): Unit = stream match {
      case Stream.Empty =>

      case head #:: tail =>
        processLine(result, head, index)
        processLines(result, tail, index + 16)
    }

    processLines(result, stream, 0)
  }

  def dump(result: StringBuilder, data: Array[Byte]): Unit =
    dump(result, data, 0, data.length)

  def dump(data: Array[Byte], offset: Int, length: Int): StringBuilder = {
    val result = new StringBuilder()
    dump(result, data, offset, length)
    result
  }

  def dump(data: Array[Byte]): StringBuilder =
    dump(data, 0, data.length)

  private val lineFormat = "[^:]*:?([\\s-0-9A-Fa-f]+)\\|?".r
  private val hexFormat = "([0-9A-Fa-f]{2})".r

  def undump(dump: String): Array[Byte] = {
    val data = for (lineFormat(data) <- lineFormat findAllIn dump;
        hexFormat(hex) <- hexFormat findAllIn data)
      yield hex
    val hash: Hash = data.mkString("")
    hash.bytes
  }

}
