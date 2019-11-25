/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2019 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import Prop._
import Arbitrary._
import java.util.concurrent.TimeUnit

object ArbitrarySpecification extends Properties("Arbitrary") {
  private[this] val genOptionUnits =
    for {
      a <- arbitrary[Option[Unit]]
      b <- arbitrary[Option[Unit]]
    } yield (a, b)

  property("arbOption coverage") =
    exists(genOptionUnits) { case (a, b) => a.isDefined != b.isDefined }

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
    Gen.listOfN(100, arbitrary[TimeUnit]).sample.get.toSet == TimeUnit.values.toSet
  }
}
