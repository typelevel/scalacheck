/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2013 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import Prop.{
  forAll, falsified, undecided, exception, passed, proved, all,
  atLeastOne, sizedProp, someFailing, noneFailing, Undecided, False, True,
  Exception, Proof, within, throws, BooleanOperators
}
import Gen.{
  const, fail, frequency, oneOf, choose, listOf, listOfN,
  Parameters
}
import java.util.concurrent.atomic.AtomicBoolean

object PropSpecification extends Properties("Prop") {

  def propException(): Prop = {
    throw new java.lang.Exception("exception")
    passed
  }

  property("Prop.==> undecided") = forAll { p1: Prop =>
    val g = oneOf(falsified,undecided)
    forAll(g) { p2 =>
      val p3 = (p2 ==> p1)
      p3 == undecided || (p3 == exception && p1 == exception)
    }
  }

  property("Prop.==> true") = {
    val g1 = oneOf(proved,falsified,undecided,exception)
    val g2 = oneOf(passed,proved)
    forAll(g1, g2) { case (p1,p2) =>
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
    def loopForever: Nothing = { println("looping"); loopForever }

    (n > 0) ==> positiveDomain(n)
  }

  property("Prop.==> exception") = {
    (passed ==> propException()) == exception
  }

  property("Prop.&& Commutativity") = {
    val g = oneOf(proved,passed,falsified,undecided,exception)
    forAll(g,g) { case (p1,p2) => (p1 && p2) == (p2 && p1) }
  }
  property("Prop.&& Exception") = forAll { p: Prop =>
    (p && propException) == exception
  }
  property("Prop.&& Exception 2") = {
    (passed && propException) == exception
  }
  property("Prop.&& Identity") = {
    val g = oneOf(proved,passed,falsified,undecided,exception)
    forAll(g)(p => (p && proved) == p)
  }
  property("Prop.&& False") = forAll { p: Prop =>
    val q = p && falsified
    q == falsified || (q == exception && p == exception)
  }
  property("Prop.&& Undecided") = {
    val g = oneOf(proved,passed,undecided)
    forAll(g)(p => (p && undecided) == undecided)
  }
  property("Prop.&& Right prio") = forAll { (sz: Int, prms: Parameters) =>
    val p = proved.map(_.label("RHS")) && proved.map(_.label("LHS"))
    p(prms).labels.contains("RHS")
  }

  property("Prop.|| Commutativity") = {
    val g = oneOf(proved,passed,falsified,undecided,exception)
    forAll(g,g) { case (p1,p2) => (p1 || p2) == (p2 || p1) }
  }
  property("Prop.|| Exception") = forAll { p: Prop =>
    (p || propException()) == exception
  }
  property("Prop.|| Identity") = {
    val g = oneOf(proved,passed,falsified,undecided,exception)
    forAll(g)(p => (p || falsified) == p)
  }
  property("Prop.|| True") = {
    val g = oneOf(proved,passed,falsified,undecided)
    forAll(g)(p => (p || proved) == proved)
  }
  property("Prop.|| Undecided") = {
    val g = oneOf(falsified,undecided)
    forAll(g)(p => (p || undecided) == undecided)
  }

  property("Prop.++ Commutativity") = {
    val g = oneOf(proved,passed,falsified,undecided,exception)
    forAll(g,g) { case (p1,p2) => (p1 ++ p2) == (p2 ++ p1) }
  }
  property("Prop.++ Exception") = forAll { p: Prop =>
    (p ++ propException()) == exception
  }
  property("Prop.++ Identity 1") = {
    val g = oneOf(falsified,passed,proved,exception)
    forAll(g)(p => (p ++ proved) == p)
  }
  property("Prop.++ Identity 2") = {
    val g = oneOf(proved,passed,falsified,undecided,exception)
    forAll(g)(p => (p ++ undecided) == p)
  }
  property("Prop.++ False") = {
    val g = oneOf(falsified,passed,proved,undecided)
    forAll(g)(p => (p ++ falsified) == falsified)
  }

  property("undecided") = forAll { prms: Parameters =>
    undecided(prms).status == Undecided
  }

  property("falsified") = forAll { prms: Parameters =>
    falsified(prms).status == False
  }

  property("proved") = forAll((prms: Parameters) => proved(prms).status == Proof)

  property("passed") = forAll((prms: Parameters) => passed(prms).status == True)

  property("exception") = forAll { (prms: Parameters, e: Throwable) =>
    exception(e)(prms).status == Exception(e)
  }

  property("all") = forAll(Gen.nonEmptyListOf(const(proved)))(l => all(l:_*))

  property("atLeastOne") = forAll(Gen.nonEmptyListOf(const(proved))) { l =>
    atLeastOne(l:_*)
  }

  property("throws") = forAll { n: Int =>
    if (n == 0) throws(classOf[ArithmeticException]) { 1/0 }
    else true
  }

  property("within") = forAll(oneOf(10, 100), oneOf(10, 100)) { (timeout: Int, sleep: Int) =>
    (timeout >= 0 && sleep >= 0) ==> {
      val q = within(timeout)(passed.map(r => {
        Thread.sleep(sleep)
        r
      }))

      if(sleep < 0.9*timeout) q == passed
      else if (sleep < 1.1*timeout) passed
      else q == falsified
    }
  }

  property("sizedProp") = {
    val g = oneOf(passed,falsified,undecided,exception)
    forAll(g) { p => p == sizedProp(_ => p) }
  }

  property("someFailing") = {
    val g: Gen[Gen[Int]] = oneOf(List(const(1), fail))
    val gs: Gen[List[Gen[Int]]] = listOf(g)
    forAll(gs) { (gs: List[Gen[Int]]) =>
      someFailing(gs) || gs.forall(_.sample.isDefined)
    }
  }

  property("noneFailing") = {
    val g: Gen[Gen[Int]] = oneOf(List(const(1), fail))
    val gs: Gen[List[Gen[Int]]] = listOf(g)
    forAll(gs) { (gs: List[Gen[Int]]) =>
      noneFailing(gs) || gs.exists(!_.sample.isDefined)
    }
  }
}
