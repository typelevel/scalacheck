/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2019 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import Prop.{
  forAll, falsified, undecided, exception, passed, proved, all,
  atLeastOne, sizedProp, someFailing, noneFailing, Undecided, False, True,
  Exception, Proof, throws, propBoolean, secure, delay, lzy
}
import Gen.{ const, fail, oneOf, listOf, Parameters }

object PropSpecification extends Properties("Prop") {

  def propException(): Prop = {
    throw new java.lang.Exception("exception")
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

  property("throws") = throws(classOf[java.lang.Exception]) {
    val it: Iterator[Int] = Iterator.empty
    it.next()
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

  property("secure") = forAll { (prms: Parameters, e: Throwable) =>
    secure(throw e).apply(prms).status == Exception(e)
  }

  property("delay") = { delay(???); proved }

  property("lzy") = { lzy(???); proved }

  property("Gen.Parameters.withInitialSeed is deterministic") =
    forAll { (p: Prop) =>
      val params = Gen.Parameters.default.withInitialSeed(999L)
      val x = p(params).success
      val set = (1 to 10).map(_ => p(params).success).toSet
      Prop(set == Set(x)).label(s"$set == Set($x)")
    }

  property("prop.useSeed is deterministic") =
    forAll { (p0: Prop, n: Long) =>
      val params = Gen.Parameters.default
      val p = p0.useSeed("some name", rng.Seed(n))
      val x = p(params).success
      val set = (1 to 10).map(_ => p(params).success).toSet
      Prop(set == Set(x)).label(s"$set == Set($x)")
    }

  property("prop.useSeed is deterministic (pt. 2)") =
    forAll { (g1: Gen[Int], g2: Gen[Int], g3: Gen[Int], n: Long) =>
      val params = Gen.Parameters.default
      val p0 = Prop.forAll(g1, g2, g3) { (x, y, z) => x == y && y == z }
      val p = p0.useSeed("some name", rng.Seed(n))
      val r1 = p(params).success
      val r2 = p(params).success
      Prop(r1 == r2).label(s"$r1 == $r2")
    }
}
