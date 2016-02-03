/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2016 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import rng.Seed

import Gen._
import Prop.{forAll, someFailing, noneFailing, sizedProp, secure, propBoolean}
import Arbitrary._
import Shrink._
import java.util.Date

object GenSpecification extends Properties("Gen") {

  implicit val arbSeed: Arbitrary[Seed] = Arbitrary(
    arbitrary[Long] flatMap Seed.apply
  )

  property("sequence") =
    forAll(listOf(frequency((10,const(arbitrary[Int])),(1,const(fail)))))(l =>
      (someFailing(l) && (sequence[List[Int],Int](l) == fail)) ||
      (noneFailing(l) && forAll(sequence[List[Int],Int](l)) { _.length == l.length })
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

  property("delay") = forAll((g: Gen[Int]) => delay(g) == g)

  property("retryUntil") = forAll((g: Gen[Int]) => g.retryUntil(_ => true) == g)

  property("const") = forAll { (x:Int, prms:Parameters, seed: Seed) =>
    const(x)(prms, seed) == Some(x)
  }

  property("fail") = forAll { (prms: Parameters, seed: Seed) =>
    fail(prms, seed) == None
  }

  property("fromOption") = forAll { (prms: Parameters, seed: Seed, o: Option[Int]) =>
    o match {
      case Some(x) => fromOption(o)(prms, seed) == Some(x)
      case None => fromOption(o)(prms, seed) == None
    }
  }

  property("collect") = forAll { (prms: Parameters, o: Option[Int], seed: Seed) =>
    val g = const(o).collect { case Some(n) => n }
    o match {
      case Some(x) => g(prms, seed) == Some(x)
      case None => g(prms, seed) == None
    }
  }

  property("choose-int") = forAll { (l: Int, h: Int) =>
    if(l > h) choose(l,h) == fail
    else forAll(choose(l,h)) { x => x >= l && x <= h }
  }

  property("choose-long") = forAll { (l: Long, h: Long) =>
    if (l > h) choose(l,h) == fail
    else forAll(choose(l,h)) { x => x >= l && x <= h }
  }

  property("choose-double") = forAll { (l: Double, h: Double) =>
    forAll(choose(l,h)) { x => x >= l && x <= h }
  }

  property("choose-large-double") = forAll(choose(Double.MinValue, Double.MaxValue)) { x =>
    x >= Double.MinValue && x <= Double.MaxValue
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

  property("calendar") = forAll(calendar) { cal =>
    cal.setLenient(false) // will cause getTime to throw if invalid
    cal.getTime != null
  }

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

  property("some") = forAll { n: Int =>
    forAll(some(n)) {
      case Some(m) if m == n => true
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

  property("suchThat combined #98") = forAll(suchThatGen) { str: String =>
    !(str.isEmpty || str.contains(','))
  }

  property("suchThat 1 #98") = forAll(suchThatGen) { str: String =>
    !str.isEmpty
  }

  property("suchThat 2 #98") = forAll(suchThatGen) { str: String =>
    !str.contains(',')
  }
  ////

  case class Full22(
    i1:Int,i2:Int,i3:Int,i4:Int,i5:Int,i6:Int,i7:Int,i8:Int,i9:Int,i10:Int,
    i11:Int,i12:Int,i13:Int,i14:Int,i15:Int,i16:Int,i17:Int,i18:Int,i19:Int,i20:Int,
    i21:Int,i22:Int
  )

  property("22 field case class works") = forAll(Gen.resultOf(Full22.tupled)){
    Full22.unapply(_).get.isInstanceOf[Tuple22[_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_]]
  }

  type Trilean = Either[Unit, Boolean]

  val tf: List[Boolean] = List(true, false)
  val utf: List[Trilean] = List(Left(()), Right(true), Right(false))

  def exhaust[A: Cogen, B: Arbitrary](n: Int, as: List[A], bs: List[B]): Boolean = {
    val fs = listOfN(n, arbitrary[A => B]).sample.get
    val outcomes = for { f <- fs; a <- as } yield (a, f(a))
    val expected = for { a <- as; b <- bs } yield (a, b)
    outcomes.toSet == expected.toSet
  }

  // none of these should fail more than 1 in 100000000 runs.
  val N = 150
  property("random (Boolean => Boolean) functions") = exhaust(N, tf, tf)
  property("random (Boolean => Trilean) functions") = exhaust(N, tf, utf)
  property("random (Trilean => Boolean) functions") = exhaust(N, utf, tf)
  property("random (Trilean => Trilean) functions") = exhaust(N, utf, utf)

  property("oneOf with Buildable supports null in first or 2nd position") = secure {
    Gen.oneOf(Gen.const(null), Arbitrary.arbitrary[Array[Byte]]).sample.isDefined &&
    Gen.oneOf(Arbitrary.arbitrary[Array[Byte]], Gen.const(null)).sample.isDefined
  }

  //// See https://github.com/rickynils/scalacheck/issues/209
  property("uniform double #209") =
    Prop.forAllNoShrink(Gen.choose(1000000, 2000000)) { n =>
      var i = 0
      var sum = 0d
      var seed = rng.Seed(n)
      while (i < n) {
        val (d,s1) = seed.double
        sum += d
        i += 1
        seed = s1
      }
      val avg = sum / n
      s"average = $avg" |: avg >= 0.49 && avg <= 0.51
    }

  property("uniform long #209") = {
    val scale = 1d / Long.MaxValue
    Prop.forAllNoShrink(Gen.choose(1000000, 2000000)) { n =>
      var i = 0
      var sum = 0d
      var seed = rng.Seed(n)
      while (i < n) {
        val (l,s1) = seed.long
        sum += math.abs(l).toDouble * scale
        i += 1
        seed = s1
      }
      val avg = sum / n
      s"average = $avg" |: avg >= 0.49 && avg <= 0.51
    }
  }
  ////
}
