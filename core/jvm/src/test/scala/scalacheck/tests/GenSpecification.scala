/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package scalacheck.tests

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import org.scalacheck.{Arbitrary, Cogen, Gen, Prop, Properties}
import org.scalacheck.rng.Seed
import org.scalacheck.Gen._
import org.scalacheck.Prop.{forAll, forAllNoShrink, someFailing, noneFailing, sizedProp, secure, propBoolean}
import org.scalacheck.Arbitrary._
import org.scalacheck.Shrink._

object GenSpecification extends Properties("Gen") with GenSpecificationVersionSpecific {

  implicit val arbSeed: Arbitrary[Seed] = Arbitrary(
    arbitrary[Long].flatMap(Seed.apply)
  )

  property("pureApply #300") = {
    def testCase[A](gen: Gen[A]): Prop =
      sizedProp { sz =>
        val g = Gen.function1(gen)(Cogen[Int])
        if (sz > 0) forAll(g) { f => f(999); true }
        else Prop(true)
      }
    val p0 = testCase(arbitrary[Int].suchThat(_ != 0))
    val p1 = testCase(arbitrary[String].suchThat(_ != ""))
    val p2 = testCase(arbitrary[Boolean].suchThat(_ != false))
    val p3 = testCase(arbitrary[List[Double]].suchThat(_ != Nil))
    val p4 = testCase(oneOf(1, 2, 3, 4, 5).suchThat(_ == 1))
    p0 && p1 && p2 && p3 && p4
  }

  property("sequence") =
    forAll(listOf(frequency((10, const(arbitrary[Int])), (1, const(fail)))))(l =>
      (someFailing(l) && (sequence[List[Int], Int](l) == fail)) ||
        (noneFailing(l) && forAll(sequence[List[Int], Int](l)) { _.length == l.length }))

  property("frequency 1") = {
    val g = frequency((10, const(0)), (5, const(1)))
    forAll(g) { _ => true }
  }

  property("frequency 2") = {
    val g = frequency((10, 0), (5, 1))
    forAll(g) { _ => true }
  }

  property("frequency 3") = forAll(choose(1, 100000)) { n =>
    forAll(frequency(List.fill(n)((1, const(0))): _*)) { _ == 0 }
  }

  property("frequency 4") =
    Prop.throws(classOf[IllegalArgumentException]) {
      frequency()
    }

  property("lzy") = forAll((g: Gen[Int]) => lzy(g) == g)

  property("wrap") = forAll((g: Gen[Int]) => delay(g) == g)

  property("delay") = forAll((g: Gen[Int]) => delay(g) == g)

  property("retryUntil") = forAll((g: Gen[Int]) => g.retryUntil(_ => true) == g)

  property("retryUntil doesn't run forever") =
    forAll((g: Gen[Int]) => Try(g.retryUntil(_ => false).sample).isFailure)

  property("retryUntil requires valid parameters") =
    forAll((g: Gen[Int]) => Try(g.retryUntil(_ => false, 0)).isFailure)

  property("const") = forAll { (x: Int, prms: Parameters, seed: Seed) =>
    const(x)(prms, seed) == Some(x)
  }

  property("fail") = forAll { (prms: Parameters, seed: Seed) =>
    fail(prms, seed) == None
  }

  property("parameterized") = forAll((g: Gen[Int]) => parameterized(_ => g) == g)

  property("sized") = forAll((g: Gen[Int]) => sized(_ => g) == g)

  property("resize(sz, posNum)") = forAll { (sz: Int) =>
    val g = Gen.resize(sz, Gen.posNum[Int])
    forAllNoShrink(g) { n =>
      if (sz > 0) n <= sz && n >= 0
      else n == 1
    }
  }

  property("resize(sz, negNum)") = forAll { (sz: Int) =>
    val g = Gen.resize(sz, Gen.negNum[Int])
    forAllNoShrink(g) { n =>
      if (sz > 0) n >= -sz && n <= 0
      else n == -1
    }
  }

  property("resize(sz, buildableOf)") = {
    val g = Gen.size.flatMap(sz => Gen.oneOf(-sz, sz))
    val gs = Gen.buildableOf[Seq[Int], Int](Arbitrary.arbitrary[Int])
    Prop.forAll(g) { (sz: Int) =>
      forAllNoShrink(Gen.resize(sz, gs)) { (l) =>
        if (sz > 0) l.size <= sz && l.size >= 0
        else l.size == 0
      }
    }
  }

  property("resize(sz, nonEmptyBuilableOf)") = {
    val g = Gen.size.flatMap(sz => Gen.oneOf(-sz, sz))
    val gs = Gen.nonEmptyBuildableOf[Seq[Int], Int](Arbitrary.arbitrary[Int])
    Prop.forAll(g) { (sz: Int) =>
      forAllNoShrink(Gen.resize(sz, gs)) { (l) =>
        if (sz > 0) l.size <= sz && l.size >= 1
        else l.size == 1
      }
    }
  }

  property("stringOf") = {
    val g = Gen.size.flatMap(sz => Gen.oneOf(-sz, sz))
    forAll(g, Gen.alphaChar) { (sz: Int, c: Char) =>
      forAllNoShrink(Gen.resize(sz, Gen.stringOf(c))) { (s) =>
        if (sz > 0) sz >= s.size && s.size >= 0 && s.forall(_ == c)
        else s.size == 0
      }
    }
  }

  property("stringOfN") = {
    val g = Gen.size.flatMap(sz => Gen.oneOf(-sz, sz))
    forAll(g, Gen.alphaChar) { (sz: Int, c: Char) =>
      forAllNoShrink(Gen.stringOfN(sz, c)) { (s) =>
        if (sz > 0) s.size == sz && s.forall(_ == c)
        else s.size == 0
      }
    }
  }

  property("nonEmptyStringOf") = {
    val g = Gen.size.flatMap(sz => Gen.oneOf(-sz, sz))
    forAll(g, Gen.alphaChar) { (sz: Int, c: Char) =>
      forAllNoShrink(Gen.resize(sz, Gen.nonEmptyStringOf(c))) { (s) =>
        if (sz > 0) sz >= s.size && s.size >= 1 && s.forall(_ == c)
        else s.size == 1
      }
    }
  }

  property("oneOf n") = forAll { (l: List[Int]) =>
    Try(oneOf(l)) match {
      case Success(g) => forAll(g)(l.contains)
      case Failure(_) => Prop(l.isEmpty)
    }
  }

  property("oneOf n in set") = forAll { (s: Set[Int]) =>
    Try(oneOf(s)) match {
      case Success(g) => forAll(g)(s.contains)
      case Failure(_) => Prop(s.isEmpty)
    }
  }

  property("oneOf 2") = forAll { (n1: Int, n2: Int) =>
    forAll(oneOf(n1, n2)) { n => n == n1 || n == n2 }
  }

  property("oneOf 2 gens") = forAll { (n1: Int, n2: Int) =>
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
      l.length > 0 && l.length <= math.max(1, sz)
    }
  }

  property("listOfN") = forAll(choose(0, 100)) { n =>
    forAll(listOfN(n, arbitrary[Int])) { _.length == n }
  }

  property("setOfN") = forAll(choose(0, 100)) { n =>
    forAll(containerOfN[Set, Int](n, arbitrary[Int])) { _.size <= n }
  }

  property("mapOfN") = forAll(choose(0, 100)) { n =>
    forAll(mapOfN(n, arbitrary[(Int, Int)])) { _.size <= n }
  }

  property("empty listOfN") = forAll(listOfN(0, arbitrary[Int])) { l =>
    l.length == 0
  }

  property("listOf(posNum)") = {
    forAll(listOf(posNum[Int])) { l =>
      l.length >= 0 && l.forall(_ > 0)
    }
  }

  property("listOf(listOf(posNum)") = { // #568
    forAll(listOf(listOf(posNum[Int]))) { ll =>
      ll.length >= 0 && ll.forall(_.length >= 0)
    }
  }

  property("nonEmptyListOf(nonEmptyListOf(posNum))") = { // #568
    forAll(nonEmptyListOf(nonEmptyListOf(posNum[Int]))) { ll =>
      ll.length > 0 && ll.forall(_.length > 0)
    }
  }

  property("listOfN(listOfN(posNum))") = sizedProp { sz => // #568
    forAll(choose(0, sz), choose(0, sz)) { (m: Int, n: Int) =>
      forAll(listOfN(m, listOfN(n, posNum[Int]))) { ll =>
        ll.length == m && ll.forall(_.length == n)
      }
    }
  }

  property("infiniteStream") = forAll(infiniteStream(arbitrary[Int]), arbitrary[Short]) { (s, n) =>
    s.drop(n & 0xffff).nonEmpty
  }

  property("infiniteLazyList") = forAll(infiniteLazyList(arbitrary[Int]), arbitrary[Short]) { (s, n) =>
    s.drop(n & 0xffff).nonEmpty
  }

  property("someOf") = forAll { (l: List[Int]) =>
    forAll(someOf(l))(_.toList.forall(l.contains))
  }

  property("pick") = forAll { (lst: List[Int]) =>
    forAll(choose(-1, 2 * lst.length)) { n =>
      Try(pick(n, lst)) match {
        case Success(g) =>
          forAll(g) { m => m.length == n && m.forall(lst.contains) }
        case Failure(_) =>
          Prop(n < 0 || n > lst.length)
      }
    }
  }

  property("pick with gen") = forAll { (x: Int, y: Int, rest: List[Int]) =>
    val lst = x :: y :: rest
    forAll(choose(-1, 2 * lst.length)) { n =>
      val gs = rest.map(Gen.const)
      Try(pick(n, Gen.const(x), Gen.const(y), gs: _*)) match {
        case Success(g) =>
          forAll(g) { m => m.length == n && m.forall(lst.contains) }
        case Failure(_) =>
          Prop(n < 0 || n > lst.length)
      }
    }
  }

  /** Expect:
    * {{{
    * 25% 1, 2, 3
    * 25% 1, 2, 4
    * 25% 1, 4, 3
    * 25% 4, 2, 3
    * }}}
    */
  property("distributed pick") = {
    val lst = (1 to 4).toIterable
    val n = 3
    forAll(pick(n, lst)) { (xs: collection.Seq[Int]) =>
      xs.map { (x: Int) =>
        Prop.collect(x) {
          xs.size == n
        }
      } reduce (_ && _)
    }
  }

  property("numChar") = forAll(numChar)(_.isDigit)

  property("calendar") = forAll(calendar) { cal =>
    cal.setLenient(false) // will cause getTime to throw if invalid
    cal.getTime != null
  }

  property("deterministic calendar") = forAll { (seed: Seed) =>
    val params = Gen.Parameters.default
    val date0 = Gen.calendar(params, seed)
    // we wait a few milliseconds to be sure we aren't accidentally
    // leaving the calendar's time unset. Calendar.getInstance starts
    // with the "current time" so if we aren't careful we will end up
    // with non-deterministic calendar generation.
    Thread.sleep(3)
    val date1 = Gen.calendar(params, seed)
    date0 == date1
  }

  property("alphaUpperChar") = forAll(alphaUpperChar) { c =>
    c.isLetter && c.isUpper
  }

  property("alphaLowerChar") = forAll(alphaLowerChar) { c =>
    c.isLetter && c.isLower
  }

  property("alphaChar") = forAll(alphaChar)(_.isLetter)

  property("alphaNumChar") = forAll(alphaNumChar)(_.isLetterOrDigit)

  property("asciiChar") = forAll(asciiChar)(_.isValidChar)

  property("asciiPrintableChar") = forAll(asciiPrintableChar) { ch =>
    val charType = Character.getType(ch)
    Character.isLetterOrDigit(ch) || Character.isSpaceChar(ch) ||
    charType == Character.CONNECTOR_PUNCTUATION || charType == Character.DASH_PUNCTUATION ||
    charType == Character.START_PUNCTUATION || charType == Character.END_PUNCTUATION ||
    charType == Character.INITIAL_QUOTE_PUNCTUATION || charType == Character.FINAL_QUOTE_PUNCTUATION ||
    charType == Character.OTHER_PUNCTUATION ||
    charType == Character.MATH_SYMBOL || charType == Character.CURRENCY_SYMBOL ||
    charType == Character.MODIFIER_SYMBOL || charType == Character.OTHER_SYMBOL
  }

  property("hexChar") = forAll(hexChar) { ch =>
    val l: Long = java.lang.Long.parseLong(ch.toString, 16)
    l < 16 && l >= 0
  }

  property("identifier") = forAll(identifier) { s =>
    s.length > 0 && s.charAt(0).isLetter && s.charAt(0).isLower &&
    s.forall(_.isLetterOrDigit)
  }

  property("numStr") = forAll(numStr) { s =>
    s.length >= 0 && s.forall(_.isDigit)
  }

  property("alphaUpperStr") = forAll(alphaUpperStr) { s =>
    s.length >= 0 && s.forall(_.isUpper)
  }

  property("alphaLowerStr") = forAll(alphaLowerStr) { s =>
    s.length >= 0 && s.forall(_.isLower)
  }

  property("alphaStr") = forAll(alphaStr) { s =>
    s.length >= 0 && s.forall(_.isLetter)
  }

  property("alphaNumStr") = forAll(alphaNumStr) { s =>
    s.length >= 0 && s.forall(_.isLetterOrDigit)
  }

  property("asciiStr") = forAll(asciiStr) { s =>
    s.length >= 0 && s.forall(_.isValidChar)
  }

  property("asciiPrintableStr") = forAll(asciiPrintableStr) { s =>
    s.length >= 0 && s.forall { ch =>
      val charType = Character.getType(ch)
      Character.isLetterOrDigit(ch) || Character.isSpaceChar(ch) ||
      charType == Character.CONNECTOR_PUNCTUATION || charType == Character.DASH_PUNCTUATION ||
      charType == Character.START_PUNCTUATION || charType == Character.END_PUNCTUATION ||
      charType == Character.INITIAL_QUOTE_PUNCTUATION || charType == Character.FINAL_QUOTE_PUNCTUATION ||
      charType == Character.OTHER_PUNCTUATION ||
      charType == Character.MATH_SYMBOL || charType == Character.CURRENCY_SYMBOL ||
      charType == Character.MODIFIER_SYMBOL || charType == Character.OTHER_SYMBOL
    }
  }

  property("hexStr") = forAll(hexStr) { s =>
    if (s.size > 0) {
      Try(BigInt(new java.math.BigInteger(s, 16))) match {
        case Success(bi) => bi >= BigInt(0L)
        case _ => false
      }
    } else {
      true
    }
  }

  // BigDecimal generation is tricky; just ensure that the generator gives
  // its constructor valid values.
  property("BigDecimal") = forAll { (_: BigDecimal) => true }

  property("resultOf1") = forAll(resultOf((_: Int) => 0))(_ == 0)

  property("resultOf2") = {
    case class A(m: Int, s: String)
    forAll(resultOf(A.apply _)) { _ => true }
  }

  property("resultOf3") = {
    case class B(n: Int, s: String, b: Boolean)
    implicit val arbB: Arbitrary[B] = Arbitrary(resultOf(B.apply _))
    forAll { (_: B) => true }
  }

  property("option") = forAll { (n: Int) =>
    forAll(option(n)) {
      case Some(m) if m == n => true
      case None => true
      case _ => false
    }
  }

  property("some") = forAll { (n: Int) =>
    forAll(some(n)) {
      case Some(m) => m == n
      case _ => false
    }
  }

  property("tailRecM") = forAll { (init: Int, seeds: List[Seed]) =>
    val g: ((Int, Int)) => Gen[Either[(Int, Int), Int]] = {
      case (c, x) if c <= 0 =>
        Gen.const(Right(x))
      case (c, x) =>
        val g = Gen.choose(Int.MinValue, x)
        g.map { i => Left(((c - 1), i)) }
    }

    val g1 = Gen.tailRecM((10, init))(g)
    def g2(x: (Int, Int)): Gen[Int] = g(x).flatMap {
      case Left(y) => g2(y)
      case Right(x) => Gen.const(x)
    }

    val finalG2 = g2((10, init))

    val params = Gen.Parameters.default

    seeds.forall { seed =>
      g1.pureApply(params, seed) == finalG2.pureApply(params, seed)
    }
  }

  property("recursive == lzy") = forAll { (seeds: List[Seed]) =>
    lazy val lzyGen: Gen[List[Int]] = {
      Gen.choose(0, 10).flatMap { idx =>
        if (idx < 5) lzyGen.map(idx :: _)
        else Gen.const(idx :: Nil)
      }
    }

    val recGen =
      Gen.recursive[List[Int]] { recurse =>
        Gen.choose(0, 10).flatMap { idx =>
          if (idx < 5) recurse.map(idx :: _)
          else Gen.const(idx :: Nil)
        }
      }

    val params = Gen.Parameters.default
    seeds.forall { seed =>
      lzyGen.pureApply(params, seed) == recGen.pureApply(params, seed)
    }
  }

  property("uuid version 4") = forAll(uuid) { _.version == 4 }

  property("uuid unique") = forAll(uuid, uuid) {
    case (u1, u2) => u1 != u2
  }

  property("zip9") = forAll(zip(
    const(1),
    const(2),
    const(3),
    const(4),
    const(5),
    const(6),
    const(7),
    const(8),
    const(9)
  )) {
    _ == ((1, 2, 3, 4, 5, 6, 7, 8, 9))
  }

  //// See https://github.com/typelevel/scalacheck/issues/79
  property("issue #79") = {
    val g = oneOf(const(0).suchThat(_ => true), const("0").suchThat(_ => true))
    forAll(g) { o => o == 0 || o == "0" }
  }
  ////

  //// See https://github.com/typelevel/scalacheck/issues/98
  private val suchThatGen = arbitrary[String]
    .suchThat(!_.isEmpty)
    .suchThat(!_.contains(','))

  property("suchThat combined #98") = forAll(suchThatGen) { (str: String) =>
    !(str.isEmpty || str.contains(','))
  }

  property("suchThat 1 #98") = forAll(suchThatGen) { (str: String) =>
    !str.isEmpty
  }

  property("suchThat 2 #98") = forAll(suchThatGen) { (str: String) =>
    !str.contains(',')
  }
  ////

  case class Full22(
      i1: Int,
      i2: Int,
      i3: Int,
      i4: Int,
      i5: Int,
      i6: Int,
      i7: Int,
      i8: Int,
      i9: Int,
      i10: Int,
      i11: Int,
      i12: Int,
      i13: Int,
      i14: Int,
      i15: Int,
      i16: Int,
      i17: Int,
      i18: Int,
      i19: Int,
      i20: Int,
      i21: Int,
      i22: Int
  )

  property("22 field case class works") =
    forAll(Gen.resultOf((Full22.apply _).tupled)) { _ => true }

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

  //// See https://github.com/typelevel/scalacheck/issues/209
  property("uniform double #209") =
    forAllNoShrink(Gen.choose(1000000, 2000000)) { n =>
      var i = 0
      var sum = 0d
      var seed = Seed(n.toLong)
      while (i < n) {
        val (d, s1) = seed.double
        sum += d
        i += 1
        seed = s1
      }
      val avg = sum / n
      s"average = $avg" |: avg >= 0.49 && avg <= 0.51
    }

  property("uniform long #209") = {
    val scale = 1d / Long.MaxValue
    forAllNoShrink(Gen.choose(1000000, 2000000)) { n =>
      var i = 0
      var sum = 0d
      var seed = Seed(n.toLong)
      while (i < n) {
        val (l, s1) = seed.long
        sum += math.abs(l).toDouble * scale
        i += 1
        seed = s1
      }
      val avg = sum / n
      s"average = $avg" |: avg >= 0.49 && avg <= 0.51
    }
  }
  ////

  property("posNum[Int]") =
    Prop.forAll(Gen.posNum[Int]) { n => n > 0 }

  property("negNum[Int]") =
    Prop.forAll(Gen.negNum[Int]) { n => n < 0 }

  property("posNum[Float]") =
    Prop.forAll(Gen.posNum[Float]) { n => n > 0.0 }

  property("negNum[Float]") =
    Prop.forAll(Gen.negNum[Float]) { n => n < 0.0 }

  property("posNum[Double] <= 1.0d") = // #439
    Prop.forAll(Gen.resize(1, Gen.posNum[Double])) { _ <= 1.0d }

  property("finite duration values are valid") =
    // just make sure it constructs valid finite values that don't throw exceptions
    Prop.forAll(Gen.finiteDuration) { _.isFinite }

  property("duration values are valid") =
    // just make sure it constructs valid values that don't throw exceptions
    Prop.forAll(Gen.duration) { _ => true }

  property("choose finite duration values are within range") = {
    val g = for {
      a <- Gen.finiteDuration
      b <- Gen.finiteDuration
    } yield if (a < b) (a, b) else (b, a)

    Prop.forAll(g) { case (low, high) =>
      Prop.forAll(Gen.choose(low, high)) { d =>
        d >= low && d <= high
      }
    }
  }

  /** Ensure that the given generator runs deterministically with the same initialSeed parameter or the same seed.
    *
    * This test should be run with a generator that can produce multiple values, and where the odds of 30 trials coming
    * back with the same result is low enough that the test won't produce many false positives.
    */
  def testDeterministicGen[A](g: Gen[A]): Prop = {
    val params0 = Gen.Parameters.default
    val params1 = params0.withInitialSeed(1248163264L)
    val seed = Seed(987654321L)
    val s0 = (1 to 30).map(_ => g(params0, Seed.random())).toSet
    val s1 = (1 to 30).map(_ => g(params1, Seed.random())).toSet
    val s2 = (1 to 30).map(_ => g(params0, seed)).toSet
    (s"$s0" |: s0.size > 1) && (s"$s1" |: s1.size == 1) && (s"$s2" |: s2.size == 1)
  }

  property("arbitrary[Boolean] is deterministic") =
    testDeterministicGen(arbitrary[Boolean])

  property("arbitrary[Long] is deterministic") =
    testDeterministicGen(arbitrary[Long])

  property("arbitrary[List[Int]] is deterministic") =
    testDeterministicGen(arbitrary[List[Int]])

  property("Gen.choose(1, 10000) is deterministic") =
    testDeterministicGen(Gen.choose(1, 10000))
}
