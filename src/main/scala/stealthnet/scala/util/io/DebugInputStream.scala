package stealthnet.scala.util.io

import java.io.{FilterInputStream, InputStream}
import stealthnet.scala.util.HexDumper
import stealthnet.scala.util.log.{Logging, LoggingContext}

/**
 * Debug input stream.
 *
 * This input stream logs read data.
 */
class DebugInputStream(
  input: InputStream,
  override protected val loggerContext: LoggingContext#LogContext
) extends FilterInputStream(input)
  with Logging
{

  override def read(): Int = {
    val result = super.read()

    if (result == -1)
      logger trace("Reached EOF")
    else
      logger trace("Read:\n" + HexDumper.dump(Array[Byte](result.asInstanceOf[Byte])))

    result
  }

  override def read(b: Array[Byte]) = read(b, 0, b.length)

  override def read(b: Array[Byte], off: Int, len: Int): Int = {
    val result = super.read(b, off, len)

    if (result == -1)
      logger trace("Reached EOF")
    else
      logger trace("Read:\n" + HexDumper.dump(b, off, result))

    result
  }

  override def skip(n: Long): Long = {
    val result = super.skip(n)

    logger trace s"Skipped: $result"

    result
  }

}
