/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package org.scalacheck

import Gen._
import Prop._
import Test._
import Arbitrary._

object TestSpecification extends Properties("Test") {

  val proved: Prop = 1 + 1 == 2

  val passing = forAll((n: Int) => n == n)

  val failing = forAll((_: Int) => false)

  val exhausted = forAll((n: Int) =>
    (n > 0 && n < 0) ==> (n == n))

  val shrunk = forAll((_: (Int, Int, Int)) => false)

  val propException = forAll { (_: Int) => throw new java.lang.Exception }

  val undefinedInt = for {
    n <- arbitrary[Int]
  } yield n / 0

  val genException = forAll(undefinedInt)((_: Int) => true)

  property("workers") = forAll { (prms: Test.Parameters) =>
    var res = true

    val cb = new Test.TestCallback {
      override def onPropEval(n: String, threadIdx: Int, s: Int, d: Int) = {
        res = res && threadIdx >= 0 && threadIdx <= (prms.workers - 1)
      }
    }

    Test.check(prms.withTestCallback(cb), passing).status match {
      case Passed => res
      case _ => false
    }
  }

  private def resultInvariant(f: (Test.Parameters, Test.Result) => Boolean): Prop =
    forAll { (prms: Test.Parameters, p: Prop) =>
      val r = Test.check(prms, p)
      f(prms, r).labelImpl2(
        s"${r.status}, s=${r.succeeded}, d=${r.discarded}, " +
          s"minSuccessful=${prms.minSuccessfulTests}, " +
          s"maxDiscardRatio=${prms.maxDiscardRatio}, " +
          s"actualDiscardRatio=${r.discarded.toFloat / r.succeeded}, " +
          s"workers=${prms.workers}"
      )
    }

  property("stopCondition") = resultInvariant { (prms, r) =>
    r.status match {
      case Passed =>
        (r.succeeded >= prms.minSuccessfulTests) &&
        (r.discarded <= prms.maxDiscardRatio * r.succeeded)
      case Exhausted =>
        (r.discarded > r.succeeded * prms.maxDiscardRatio) &&
        (r.discarded >= prms.minSuccessfulTests * prms.maxDiscardRatio)
      case _ =>
        (r.succeeded < prms.minSuccessfulTests) &&
        (r.discarded <= prms.maxDiscardRatio * r.succeeded)
    }
  }

  property("size") = forAll { (prms: Test.Parameters) =>
    val p = sizedProp { sz => sz >= prms.minSize && sz <= prms.maxSize }
    Test.check(prms, p).status == Passed
  }

  property("propFailing") = forAll { (prms: Test.Parameters) =>
    Test.check(prms, failing).status match {
      case _: Failed => true
      case _ => false
    }
  }

  property("propPassing") = forAll { (prms: Test.Parameters) =>
    Test.check(prms, passing).status == Passed
  }

  property("propProved") = forAll { (prms: Test.Parameters) =>
    Test.check(prms, proved).status match {
      case _: Test.Proved => true
      case _ => false
    }
  }

  property("propExhausted") = forAll { (prms: Test.Parameters) =>
    Test.check(prms, exhausted).status == Exhausted
  }

  property("propPropException") = forAll { (prms: Test.Parameters) =>
    Test.check(prms, propException).status match {
      case _: PropException => true
      case _ => false
    }
  }

  property("propGenException") = forAll { (prms: Test.Parameters) =>
    Test.check(prms, genException).status match {
      case x: PropException => true.labelImpl2(x.toString)
      case x => false.labelImpl2(x.toString)
    }
  }

  property("propShrunk") = forAll { (prms: Test.Parameters) =>
    Test.check(prms, shrunk).status match {
      case Failed(Arg(_, (x: Int, y: Int, z: Int), _, _, _, _) :: Nil, _) =>
        x == 0 && y == 0 && z == 0
      case _ => false
    }
  }

  property("disabling shrinking works") = {

    object Bogus {
      val gen: Gen[Bogus] =
        Gen.choose(Int.MinValue, Int.MaxValue).map(Bogus(_))

      var shrunk: Boolean = false

      implicit def shrinkBogus: Shrink[Bogus] = {
        Shrink { (_: Bogus) => shrunk = true; Stream.empty }
      }
    }

    case class Bogus(x: Int)

    val prop = Prop.forAll[Bogus, Prop](Bogus.gen) { _ => Prop(false) }
    val prms = Test.Parameters.default.disableLegacyShrinking
    val res = Test.check(prms, prop)
    Prop(!res.passed && !Bogus.shrunk)
  }

  property("Properties.overrideParameters overrides Test.Parameters") = {

    val seed0 = rng.Seed.fromBase64("aaaaa_mr05Z_DCbd2PyUolC0h93iH1MQwIdnH2UuI4L=").get
    val seed1 = rng.Seed.fromBase64("zzzzz_mr05Z_DCbd2PyUolC0h93iH1MQwIdnH2UuI4L=").get

    val myProps = new Properties("MyProps") {
      override def overrideParameters(prms: Test.Parameters): Test.Parameters =
        prms.withInitialSeed(Some(seed1))
      property("initial seed matches") =
        Prop { prms =>
          val ok = prms.initialSeed == Some(seed1)
          Prop.Result(status = if (ok) Prop.Proof else Prop.False)
        }
    }

    Prop {
      val prms = Test.Parameters.default.withInitialSeed(Some(seed0))
      val results = Test.checkProperties(prms, myProps)
      val ok = results.forall { case (_, res) => res.passed }
      Prop.Result(status = if (ok) Prop.Proof else Prop.False)
    }
  }

  property("initialSeed is used and then updated") = {
    val seed = rng.Seed.fromBase64("aaaaa_mr05Z_DCbd2PyUolC0h93iH1MQwIdnH2UuI4L=").get
    val gen = Gen.choose(Int.MinValue, Int.MaxValue)
    val expected = gen(Gen.Parameters.default, seed).get

    val prms = Test.Parameters.default
      .withInitialSeed(Some(seed))
      .withMinSuccessfulTests(10)

    var xs: List[Int] = Nil
    val prop = Prop.forAll(gen) { x =>
      xs = x :: xs
      true
    }

    val res = Test.check(prms, prop)
    val n = xs.size
    val unique = xs.toSet
    val p0 = Prop(unique(expected)).labelImpl2(s"did not see $expected in $unique")
    val p1 = Prop(unique.size > 1).labelImpl2(s"saw $n duplicate values: $unique")
    p0 && p1
  }

  property("initialSeed is used and then updated when varying RNG spins") = {
    val seed = rng.Seed.fromBase64("aaaaa_mr05Z_DCbd2PyUolC0h93iH1MQwIdnH2UuI4L=").get
    val gen = Gen.choose(Int.MinValue, Int.MaxValue)
    val expected = gen(Gen.Parameters.default, seed).get

    val prms = Test.Parameters.default
      .withInitialSeed(Some(seed))
      .withMinSuccessfulTests(10)
      .withMaxRNGSpins(5)

    var xs: List[Int] = Nil
    val prop = Prop.forAll(gen) { x =>
      xs = x :: xs
      true
    }

    Test.check_(prms, prop)
    val n = xs.size
    val unique = xs.toSet
    val p0 = Prop(unique(expected)).labelImpl2(s"did not see $expected in $unique")
    val p1 = Prop(unique.size > 1).labelImpl2(s"saw $n duplicate values: $unique")
    p0 && p1
  }
}
