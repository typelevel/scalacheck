/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2016 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import Prop.forAll
import Arbitrary.arbitrary
import rng.Seed

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

  property("longs are evenly-distributed") =
    forAll(arbitrary[Seed], Gen.choose(2, 100)) { (seed: Seed, base: Int) =>
      val buckets = new Array[Int](base)
      def loop(s0: Seed, count: Int): Unit =
        if (count > 0) {
          val (x, s1) = s0.long
          buckets((x & 0xffff).toInt % base) += 1
          loop(s1, count - 1)
        }
      loop(seed, 1000)
      base == base
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
      s0 == Seed.fromBase64(s0.toBase64)
    }

  property("illegal seeds throw exceptions") =
    forAll { (s: String) =>
      scala.util.Try(Seed.fromBase64(s)).isFailure
    }
}
