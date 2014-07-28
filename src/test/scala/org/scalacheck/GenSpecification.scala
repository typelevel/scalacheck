/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2014 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import Gen._
import Prop.{forAll, someFailing, noneFailing, sizedProp}
import Arbitrary._
import Shrink._
import java.util.Date

object GenSpecification extends Properties("Gen") {
  property("sequence") =
    forAll(listOf(frequency((10,const(arbitrary[Int])),(1,const(fail)))))(l =>
      (someFailing(l) && (sequence[List,Int](l) == fail)) ||
      (noneFailing(l) && forAll(sequence[List,Int](l)) { _.length == l.length })
    )

  property("frequency 1") = {
    val g = frequency((10, const(0)), (5, const(1)))
    forAll(g) { n => true }
  }

  property("frequency 2") = {
    val g = frequency((10, 0), (5, 1))
    forAll(g) { n => true }
  }

  property("frequency 3") = forAll(choose(0,100000)) { n =>
    forAll(frequency(List.fill(n)((1,const(0))): _*)) { _ == 0 }
  }

  property("lzy") = forAll((g: Gen[Int]) => lzy(g) == g)

  property("wrap") = forAll((g: Gen[Int]) => wrap(g) == g)

  property("retryUntil") = forAll((g: Gen[Int]) => g.retryUntil(_ => true) == g)

  property("const") = forAll((x:Int, prms:Parameters) => const(x)(prms) == Some(x))

  property("fail") = forAll((prms: Parameters) => fail(prms) == None)

  property("choose-int") = forAll { (l: Int, h: Int) =>
    if(l > h) choose(l,h) == fail
    else forAll(choose(l,h)) { x => x >= l && x <= h }
  }

  property("choose-long") = forAll { (l: Long, h: Long) =>
    if (l > h) choose(l,h) == fail
    else forAll(choose(l,h)) { x => x >= l && x <= h }
  }

  property("choose-double") = forAll { (l: Double, h: Double) =>
    if(l > h || h-l > Double.MaxValue) choose(l,h) == fail
    else forAll(choose(l,h)) { x => x >= l && x <= h }
  }

  property("choose-xmap") = {
    implicit val chooseDate = Choose.xmap[Long, Date](new Date(_), _.getTime)
    forAll { (l: Date, h: Date) =>
      if(l.after(h)) choose(l, h) == fail
      else forAll(choose(l,h)) { x => x.compareTo(l) >= 0 && x.compareTo(h) <= 0 }
    }
  }

  property("parameterized") = forAll((g: Gen[Int]) => parameterized(p=>g) == g)

  property("sized") = forAll((g: Gen[Int]) => sized(i => g) == g)

  property("oneOf n") = forAll { l: List[Int] =>
    if(l.isEmpty) oneOf(l) == fail
    else forAll(oneOf(l))(l.contains)
  }

  property("oneOf 2") = forAll { (n1:Int, n2:Int) =>
    forAll(oneOf(n1, n2)) { n => n == n1 || n == n2 }
  }

  property("oneOf 2 gens") = forAll { (n1:Int, n2:Int) =>
    val g1 = Gen.const(n1)
    val g2 = Gen.const(n2)
    forAll(oneOf(g1, g2)) { n => n == n1 || n == n2 }
  }

  property("listOf") = sizedProp { sz =>
    forAll(listOf(arbitrary[Int])) { l =>
      l.length >= 0 && l.length <= sz
    }
  }

  property("nonEmptyListOf") = sizedProp { sz =>
    forAll(nonEmptyListOf(arbitrary[Int])) { l =>
      l.length > 0 && l.length <= math.max(1,sz)
    }
  }

  property("listOfN") = forAll(choose(0,100)) { n =>
    forAll(listOfN(n, arbitrary[Int])) { _.length == n }
  }

  property("setOfN") = forAll(choose(0,100)) { n =>
    forAll(containerOfN[Set,Int](n, arbitrary[Int])) { _.size <= n }
  }

  property("mapOfN") = forAll(choose(0,100)) { n =>
    forAll(mapOfN(n, arbitrary[(Int,Int)])) { _.size <= n }
  }

  property("empty listOfN") = forAll(listOfN(0, arbitrary[Int])) { l =>
    l.length == 0
  }

  property("someOf") = forAll { l: List[Int] =>
    forAll(someOf(l))(_.toList.forall(l.contains))
  }

  property("pick") = forAll { l: List[Int] =>
    forAll(choose(-1, 2*l.length)) { n =>
      if(n < 0 || n > l.length) pick(n,l) == fail
      else forAll(pick(n,l)) { m => m.length == n && m.forall(l.contains) }
    }
  }

  property("numChar") = forAll(numChar)(_.isDigit)

  property("alphaUpperChar") = forAll(alphaUpperChar) { c =>
    c.isLetter && c.isUpper
  }

  property("alphaLowerChar") = forAll(alphaLowerChar) { c =>
    c.isLetter && c.isLower
  }

  property("alphaChar") = forAll(alphaChar)(_.isLetter)

  property("alphaNumChar") = forAll(alphaNumChar)(_.isLetterOrDigit)

  property("identifier") = forAll(identifier) { s =>
    s.length > 0 && s(0).isLetter && s(0).isLower &&
    s.forall(_.isLetterOrDigit)
  }

  // BigDecimal generation is tricky; just ensure that the generator gives
  // its constructor valid values.
  property("BigDecimal") = forAll { _: BigDecimal => true }

  property("resultOf1") = forAll(resultOf((m: Int) => 0))(_ == 0)

  property("resultOf2") = {
    case class A(m: Int, s: String)
    forAll(resultOf(A)) { a:A => true }
  }

  property("resultOf3") = {
    case class B(n: Int, s: String, b: Boolean)
    implicit val arbB = Arbitrary(resultOf(B))
    forAll { b:B => true }
  }

  property("option") = forAll { n: Int =>
    forAll(option(n)) {
      case Some(m) if m == n => true
      case None => true
      case _ => false
    }
  }

  property("uuid version 4") = forAll(uuid) { _.version == 4 }

  property("uuid unique") = forAll(uuid, uuid) {
    case (u1,u2) => u1 != u2
  }

  property("zip9") = forAll(zip(
    const(1), const(2), const(3), const(4),
    const(5), const(6), const(7), const(8),
    const(9)
  )) {
    _ == (1,2,3,4,5,6,7,8,9)
  }

  //// See https://github.com/rickynils/scalacheck/issues/79
  property("issue #79") = {
    val g = oneOf(const(0).suchThat(_ => true), const("0").suchThat(_ => true))
    forAll(g) { o => o == 0 || o == "0" }
  }
  ////

  //// See https://github.com/rickynils/scalacheck/issues/98
  private val suchThatGen = arbitrary[String]
    .suchThat(!_.isEmpty)
    .suchThat(!_.contains(','))

  property("suchThat combined") = forAll(suchThatGen) { str: String =>
    !(str.isEmpty || str.contains(','))
  }

  property("suchThat 1") = forAll(suchThatGen) { str: String =>
    !str.isEmpty
  }

  property("suchThat 2") = forAll(suchThatGen) { str: String =>
    !str.contains(',')
  }
  ////

}
