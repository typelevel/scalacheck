/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import java.util.concurrent.TimeUnit

import Prop._
import Arbitrary._

object ArbitrarySpecification extends Properties("Arbitrary") {
  val genOptionUnits =
    for {
      a <- arbitrary[Option[Unit]]
      b <- arbitrary[Option[Unit]]
    } yield (a, b)

  property("arbOption coverage") =
    exists(genOptionUnits) { case (a, b) => a.isDefined != b.isDefined }

  property("arbChar") =
    Prop.forAll { (c: Char) =>
      0x0000 <= c && c <= 0xD7FF || 0xE000 <= c && c <= 0xFFFD
    }

  property("arbString") =
    Prop.forAll { (s: String) =>
      s ne new String(s)
    }

  property("arbSymbol") =
    Prop.forAll { (s: Symbol) =>
      s eq Symbol(s.name)
    }

  case class Recur(opt: Option[Recur])

  property("arbitrary[Recur].passes") = {
    implicit def arbRecur: Arbitrary[Recur] = Arbitrary {
      Gen.delay(Arbitrary.arbitrary[Option[Recur]])
        .map(Recur(_))
    }
    Prop.forAll { (_: Recur) =>
      Prop.passed
    }
  }

  property("arbitrary[Recur].throws[StackOverflowError]") = {
    implicit def arbRecur: Arbitrary[Recur] = Arbitrary {
      Arbitrary.arbitrary[Option[Recur]]
        .map(Recur(_))
    }
    Prop.throws(classOf[java.lang.StackOverflowError]) {
      Prop.forAll { (_: Recur) =>
        Prop.passed
      }
    }
  }

  property("arbEnum") = {
    val g = Gen.containerOfN[List,TimeUnit](100, arbitrary[TimeUnit])
    Prop.forAllNoShrink(g) { (l: List[TimeUnit]) =>
      l.toSet == TimeUnit.values.toSet
    }
  }
}
