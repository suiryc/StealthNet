package stealthnet.scala.util

import scala.collection.mutable.IndexedSeqView
import stealthnet.scala.util.Hash

/**
 * Hexadecimal data dumper.
 *
 * Offers a way to represent data by their hexadecimal (and ASCII) form, for
 * example:
 * {{{
 * 0000: 1C CC 54 C6 88 85 37 88 - 8E 91 A8 19 0D 8B 56 8A | [..T...7.......V.]
 * 0010: D6 38 A8 17 A3 7E 62 A4 - 44 28 A2 76             | [.8...~b.D(.v    ]
 * }}}
 *
 * Also offers a method to rebuild binary data from an hexadecimal
 * representation.
 */
object HexDumper {

  /* The data is split in 16-bytes views. */
  private val viewBytes = 16
  /* Note: the hexadecimal view is split in two halves
   *   - there are (viewBytes / 2) bytes in each half
   *   - each byte is represented as 2 hexadecimal characters
   *   - there is one space between each represented byte: so the total
   *     number of spaces is one less than the number of represented bytes
   * Hence the length of each half hexadecimal view is (viewBytes / 2) * 3 - 1
   */
  private val hexadecimalHalfViewSize = (viewBytes / 2) * 3 - 1

  /**
   * Dumps data.
   *
   * @param result where to dump the hexadecimal representation of data
   * @param data data to dump
   * @param offset data offset
   * @param length data length
   */
  // scalastyle:off method.length
  def dump(result: StringBuilder, data: Array[Byte], offset: Int, length: Int) {

    /* Let's have some fun with Stream: the idea is to have a view of 16
     * consecutive bytes sliding over the data to dump.
     */

    type BytesView = scala.collection.IterableView[Byte, Array[Byte]]

    /** Builds the Stream, each element giving a 16-bytes view. */
    def stream: Stream[BytesView] = {
      def loop(rest: BytesView): Stream[BytesView] =
        rest.splitAt(viewBytes) match {
          case (head, tail) if (!head.isEmpty) =>
            head #:: loop(tail)

          case _ =>
            Stream.empty
        }

      loop(data.view(offset, length))
    }

    /** Padding. */
    def ensureLength(str: String, len: Int) =
      if (str.length >= len)
        str
      else
        str + " " * (len - str.length)

    /** Produces the hexadecimal representation. */
    def processHexSection(section: Array[Byte]) =
      section.map("%02X".format(_)).mkString(" ")

    /** Produces the ASCII representation. */
    def processAsciiSection(section: Array[Byte]) =
      section.map(c => if ((c >= 0x20) && (c < 0x7F)) c.asInstanceOf[Char] else '.').mkString("")

    /** Process one 'line' (that is 16-bytes view) */
    def processLine(result: StringBuilder, line: BytesView, index: Int) {
      val (first, second) = line.force.splitAt(viewBytes / 2)

      if (result.length > 0)
        result.append('\n')

      result.append("%04X: ".format(index))
        .append(ensureLength(processHexSection(first), hexadecimalHalfViewSize))
        .append(" - ")
        .append(ensureLength(processHexSection(second), hexadecimalHalfViewSize))
        .append(" | [")
        .append(ensureLength(processAsciiSection(first) + processAsciiSection(second), viewBytes))
        .append(']')
    }

    /**
     * Processes a Stream.
     *
     * @note To ensure an optimal tail-recursive call, the representation cannot
     *   be the result of this method (last operation would be a concatenation
     *   which does prevent tail recursion).
     */
    @annotation.tailrec
    def processLines(result: StringBuilder, stream: Stream[BytesView],
        index: Int): Unit =
      stream match {
        case Stream.Empty =>

        case head #:: tail =>
          processLine(result, head, index)
          processLines(result, tail, index + 16)
      }

    processLines(result, stream, 0)
  }
  // scalastyle:on method.length

  /**
   * Dumps data.
   *
   * @param data data to dump
   * @param offset data offset
   * @param length data length, negative value means whole array
   * @param result where to dump the hexadecimal representation of data
   * @return result containing the hexadecimal representation of data
   */
  def dump(data: Array[Byte], offset: Int = 0, length: Int = -1,
      result: StringBuilder = new StringBuilder()): StringBuilder =
  {
    dump(result, data, offset,
      if (length < 0) data.length - offset else length)
    result
  }

  /** Regular expression extracting hexadecimal representation. */
  private val lineFormat = """[^:]*:?([\s-0-9A-Fa-f]+)\|?""".r
  /** Regular expression extracting hexadecimal data from a representation. */
  private val hexFormat = """([0-9A-Fa-f]{2})""".r

  /**
   * Undumps data.
   *
   * @param dump hexadecimal representation of data to rebuild
   * @return original data corresponding to the given hexadecimal representation
   */
  def undump(dump: String): Array[Byte] = {
    val data = for {
      lineFormat(data) <- lineFormat findAllIn dump
      hexFormat(hex) <- hexFormat findAllIn data
    } yield hex
    (data.mkString: Hash).bytes
  }

}
