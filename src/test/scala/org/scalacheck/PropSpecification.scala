/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2011 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*-------------------------------------------------------------------------*/

package org.scalacheck

import Prop._
import Gen.{value, fail, frequency, oneOf}
import Arbitrary._
import Shrink._

object PropSpecification extends Properties("Prop") {

  property("Prop.==> undecided") = forAll { p1: Prop =>
    val g = oneOf(falsified,undecided)
    forAll(g) { p2 => 
      val p3 = (p2 ==> p1) 
      p3 == undecided || (p3 == exception && p1 == exception)
    }
  }

  property("Prop.==> true") = forAll { p1: Prop =>
    val g = oneOf(passed,proved)
    forAll(g) { p2 => 
      val p = p2 ==> p1
      (p == p1) || (p2 == passed && p1 == proved && p == passed)
    }
  }

  property("Prop.==> short circuit") = forAll { n: Int =>
    def positiveDomain(n: Int): Boolean = n match {
      case n if n > 0 => true
      case n if (n & 1) == 0 => throw new java.lang.Exception("exception")
      case _ => loopForever
    }
    def loopForever: Nothing = loopForever

    (n > 0) ==> positiveDomain(n)
  }

  property("Prop.&& Commutativity") = forAll { (p1: Prop, p2: Prop) =>
    (p1 && p2) == (p2 && p1)
  }
  property("Prop.&& Exception") = forAll { p: Prop =>
    (p && exception) == exception
  }
  property("Prop.&& Identity") = forAll { p: Prop =>
    (p && proved) == p
  }
  property("Prop.&& False") = forAll { p: Prop =>
    val q = p && falsified
    q == falsified || (q == exception && p == exception)
  }
  property("Prop.&& Undecided") = {
    val g = oneOf(proved,undecided)
    forAll(g)(p => (p && undecided) == undecided)
  }
  property("Prop.&& Right prio") = forAll { (sz: Int, prms: Params) =>
    val p = proved.map(_.label("RHS")) && proved.map(_.label("LHS"))
    p(prms).labels.contains("RHS")
  }

  property("Prop.|| Commutativity") = forAll { (p1: Prop, p2: Prop) =>
    (p1 || p2) == (p2 || p1)
  }
  property("Prop.|| Exception") = forAll { p: Prop =>
    (p || exception) == exception
  }
  property("Prop.|| Identity") = forAll { p: Prop =>
    (p || falsified) == p
  }
  property("Prop.|| True") = {
    val g = oneOf(proved,falsified,undecided)
    forAll(g)(p => (p || proved) == proved)
  }
  property("Prop.|| Undecided") = {
    val g = oneOf(falsified,undecided)
    forAll(g)(p => (p || undecided) == undecided)
  }

  property("Prop.++ Commutativity") = forAll { (p1: Prop, p2: Prop) =>
    (p1 ++ p2) == (p2 ++ p1)
  }
  property("Prop.++ Exception") = forAll { p: Prop =>
    (p ++ exception) == exception
  }
  property("Prop.++ Identity 1") = {
    val g = oneOf(falsified,proved,exception)
    forAll(g)(p => (p ++ proved) == p)
  }
  property("Prop.++ Identity 2") = forAll { p: Prop =>
    (p ++ undecided) == p
  }
  property("Prop.++ False") = {
    val g = oneOf(falsified,proved,undecided)
    forAll(g)(p => (p ++ falsified) == falsified)
  }

  property("undecided") = forAll { prms: Params => 
    undecided(prms).status == Undecided
  }

  property("falsified") = forAll { prms: Params => 
    falsified(prms).status == False
  }

  property("proved") = forAll((prms: Params) => proved(prms).status == Proof)

  property("passed") = forAll((prms: Params) => passed(prms).status == True)

  property("exception") = forAll { (prms: Params, e: Throwable) =>
    exception(e)(prms).status == Exception(e)
  }

  property("all") = forAll(Gen.listOf1(value(proved)))(l => all(l:_*))

  property("atLeastOne") = forAll(Gen.listOf1(value(proved))) { l => 
    atLeastOne(l:_*)
  }

  property("throws") = ((1/0) throws classOf[ArithmeticException])

}
