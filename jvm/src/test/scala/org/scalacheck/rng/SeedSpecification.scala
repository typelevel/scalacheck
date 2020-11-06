/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2019 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck
package rng

import Prop.forAll
import Arbitrary.arbitrary
import scala.util.Try

object SeedSpecification extends Properties("Seed") {

  implicit val arbSeed: Arbitrary[Seed] =
    Arbitrary(arbitrary[Long].flatMap(n => Seed(n)))

  property("different seeds produce different values") =
    forAll { (s: Seed) =>
      s.long._1 != s.next.long._1
    }

  property("doubles are within [0, 1)") =
    forAll { (s: Seed) =>
      val n = s.double._1
      0.0 <= n && n < 1.0
    }

  case class Base(value: Int)

  object Base {
    implicit val arbitraryBase: Arbitrary[Base] =
      Arbitrary(Gen.choose(2, 100).map(Base(_)))
  }

  property("longs are evenly-distributed") =
    forAll { (seed: Seed, b: Base) =>
      val base = b.value

      def countZeros(s0: Seed, i: Int, seen0: Int): Int =
        if (i <= 0) seen0 else {
          val (x, s1) = s0.long
          val n = (x & 0x7fffffff).toInt % base
          val seen = if (n == 0) seen0 + 1 else seen0
          countZeros(s1, i - 1, seen)
        }

      val count = 10000
      val mean = count.toDouble / base
      val stdDev = Math.sqrt(mean * ((base.toDouble - 1) / base))
      val delta = 5 * stdDev // 1 in 1.7M false positives
      val zeros = countZeros(seed, count, 0)
      (mean - delta) <= zeros && zeros <= (mean + delta)
    }

  property("equality works") =
    forAll { (s0: Seed, s1: Seed) =>
      def p(s0: Seed, s1: Seed): Boolean = (s0 == s1) == (s0.long == s1.long)
      p(s0, s0) && p(s0, s1) && p(s1, s0) && p(s1, s1)
    }

  property("reseed works") =
    forAll { (s: Seed, n: Long) =>
      s.reseed(n) != s
    }

  property("base-64 serialization works") =
    forAll { (s0: Seed) =>
      Try(s0) == Seed.fromBase64(s0.toBase64)
    }

  property("illegal seeds throw exceptions") =
    forAll { (s: String) =>
      Seed.fromBase64(s).isFailure
    }

  property("illegal seed") = Prop.throws(classOf[IllegalArgumentException])(Seed.fromLongs(0, 0, 0, 0))
}
