package perso.stealthnet.core.cryptography

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite

@RunWith(classOf[JUnitRunner])
class MessageSuite extends FunSuite {

  DigestSuite.digests
  test("SHA digests") {
    for ((message, results) <- DigestSuite.digests) {
      val m = Message(message.getBytes("US-ASCII"))
      for ((algorithm, hash) <- results) {
        val a = Hash(hash)
        val b = m.hash(algorithm)
        assert(a === b,
            "Algorithm[%s] message[%s] digest[%s] does not match expected[%s]".format(
                Algorithm.algorithm(algorithm), message, b, a)
            )

        val c = Message.hash(message.getBytes("US-ASCII"), algorithm)
        assert(a === c,
            "Algorithm[%s] message[%s] digest[%s] does not match expected[%s]".format(
                Algorithm.algorithm(algorithm), message, c, a)
            )
      }
    }
  }

}