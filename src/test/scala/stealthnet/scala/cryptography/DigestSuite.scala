package stealthnet.scala.cryptography

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

object DigestSuite {

  protected[cryptography] val digests: Map[String, Map[Algorithm.Value, String]] = Map(
      "The quick brown fox jumps over the lazy dog" -> Map(
          Algorithm.SHA1 -> "2fd4e1c67a2d28fced849ee1bb76e7391b93eb12",
          Algorithm.SHA256 -> "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592",
          Algorithm.SHA384 -> "ca737f1014a48f4c0b6dd43cb177b0afd9e5169367544c494011e3317dbf9a509cb1e5dc1e85a941bbee3d7f2afbc9b1",
          Algorithm.SHA512 -> "07e547d9586f6a73f73fbac0435ed76951218fb7d0c8d788a309d785436bbb642e93a252a954f23912547d1e8a3b5ed6e1bfd7097821233fa0538f3db854fee6"
      )
  )

}

@RunWith(classOf[JUnitRunner])
class DigestSuite extends FunSuite {

  test("SHA digests") {
    for (algorithm <- Algorithm.values) {
      var count = 0

      for ((message, results) <- DigestSuite.digests;
          (algorithm2, hash) <- results if (algorithm2 == algorithm))
      {
        val a = Hash(hash)
        val b = Digest(algorithm).update(message.getBytes("US-ASCII")).digest()
        assert(a === b,
            "Algorithm[%s] message[%s] digest[%s] does not match expected[%s]".format(
                Algorithm.algorithm(algorithm), message, b, a)
            )
        count += 1
      }

      assert(count > 0,
          "Algorithm[%s] has no digest to check against".format(Algorithm.algorithm(algorithm)))
    }
  }

}
