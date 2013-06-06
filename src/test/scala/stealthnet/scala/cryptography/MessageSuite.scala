package stealthnet.scala.cryptography

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import stealthnet.scala.util.Hash

@RunWith(classOf[JUnitRunner])
class MessageSuite extends FunSuite {

  test("SHA digests") {
    for ((message, results) <- DigestSuite.digests) {
      val m = Message(message.getBytes("US-ASCII"))
      for ((algorithm, hash) <- results) {
        val a = Hash(hash)
        val b = m.hash(algorithm)
        assert(a === b,
          s"Algorithm[${Algorithm.algorithm(algorithm)}] message[$message] digest[$b] does not match expected[$a]"
        )

        val c = Message.hash(message.getBytes("US-ASCII"), algorithm)
        assert(a === c,
          s"Algorithm[${Algorithm.algorithm(algorithm)}] message[$message] digest[$c] does not match expected[$a]"
        )
      }
    }
  }

}
