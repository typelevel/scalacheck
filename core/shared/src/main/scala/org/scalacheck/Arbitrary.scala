/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package org.scalacheck

import concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration.{Duration, FiniteDuration}

import util.Buildable
import util.SerializableCanBuildFroms._

/**
 * Define an arbitrary generator for properties
 *
 * The [[Arbitrary]] module defines implicit generator instances for
 * common types.
 *
 * The implicit definitions of [[Arbitrary]] provide type-directed
 * [[Gen]]s so they are available for properties, generators, or other
 * definitions of [[Arbitrary]].
 *
 * ScalaCheck expects an implicit [[Arbitrary]] instance is in scope
 * for [[Prop]]s that are defined with functions, like [[Prop$.forAll[T1,P](g1*
 * Prop.forAll]] and so on.
 *
 * For instance, the definition for `Arbitrary[Boolean]` is used by
 * `Prop.forAll` to automatically provide a `Gen[Boolean]` when one
 * of the parameters is a `Boolean`:
 *
 *  {{{
 *    Prop.forAll { (b: Boolean) =>
 *      b || !b
 *    }
 *  }}}
 *
 * Thanks to `Arbitrary`, you don't need to provide an explicit
 * `Gen` instance to `Prop.forAll`.  For instance, this is
 * unnecessary:
 *
 *  {{{
 *    val genBool: Gen[Boolean] = Gen.oneOf(true,false)
 *    Prop.forAll(genBool) { (b: Boolean) =>
 *      b || !b
 *    }
 *  }}}
 *
 * Since an arbitrary `Gen` for `Boolean` is defined in `Arbitrary`,
 * it can be summoned with `Arbitrary.arbitrary` in cases where you
 * need to provide one explicitly:
 *
 *  {{{
 *    val genBool: Gen[Boolean] = Arbitrary.arbitrary[Boolean]
 *    val genSmallInt: Gen[Int] = Gen.choose(0, 9)
 *    Prop.forAll(genBool, genSmallInt) { (b: Boolean, i: Int) =>
 *      i < 10 && b || !b
 *    }
 *  }}}
 *
 * For a user-defined `MyClass`, writing the following requires that
 * there exists an implicit `Arbitrary[MyClass]` instance:
 *
 *  {{{
 *    Prop.forAll { (myClass: MyClass) =>
 *      ...
 *    }
 *  }}}
 *
 * The implicit definition of `Arbitrary[MyClass]` would look like:
 *
 *  {{{
 *    implicit val arbMyClass: Arbitrary[MyClass] = Arbitrary {
 *      ...
 *    }
 *  }}}
 *
 *  The factory method `Arbitrary(...)` expects a generator of type
 *  `Gen[MyClass]` then it will return an instance of `Arbitrary[MyClass]`.
 */
sealed abstract class Arbitrary[T] extends Serializable {
  def arbitrary: Gen[T]
}

object Arbitrary extends ArbitraryLowPriority with ArbitraryArities with time.JavaTimeArbitrary {

  /** Arbitrary instance of the Function0 type. */
  implicit def arbFunction0[T](implicit a: Arbitrary[T]): Arbitrary[() => T] =
  Arbitrary(arbitrary[T].map(() => _))
}

/** separate trait to have same priority as ArbitraryArities */
private[scalacheck] sealed trait ArbitraryLowPriority {
  import Gen.{const, choose, sized, frequency, oneOf, buildableOf}

  /** Creates an Arbitrary instance */
  def apply[T](g: => Gen[T]): Arbitrary[T] = new Arbitrary[T] {
    lazy val arbitrary = g
  }

  /** Returns an arbitrary generator for the type T. */
  def arbitrary[T](implicit a: Arbitrary[T]): Gen[T] = a.arbitrary

  /**** Arbitrary instances for each AnyVal ****/

  /** Arbitrary AnyVal */
  implicit lazy val arbAnyVal: Arbitrary[AnyVal] = Arbitrary(oneOf(
    arbitrary[Unit], arbitrary[Boolean], arbitrary[Char], arbitrary[Byte],
    arbitrary[Short], arbitrary[Int], arbitrary[Long], arbitrary[Float],
    arbitrary[Double]
  ))

  /** Arbitrary instance of Boolean */
  implicit lazy val arbBool: Arbitrary[Boolean] =
    Arbitrary(oneOf(true, false))

  /** Arbitrary instance of Int */
  implicit lazy val arbInt: Arbitrary[Int] = Arbitrary(
    Gen.chooseNum(Int.MinValue, Int.MaxValue)
  )

  /** Arbitrary instance of Long */
  implicit lazy val arbLong: Arbitrary[Long] = Arbitrary(
    Gen.chooseNum(Long.MinValue, Long.MaxValue)
  )

  /** Arbitrary instance of Float */
  implicit lazy val arbFloat: Arbitrary[Float] = Arbitrary(
    for {
      s <- choose(0, 1)
      e <- choose(0, 0xfe)
      m <- choose(0, 0x7fffff)
    } yield java.lang.Float.intBitsToFloat((s << 31) | (e << 23) | m)
  )

  /** Arbitrary instance of Double */
  implicit lazy val arbDouble: Arbitrary[Double] = Arbitrary(
    for {
      s <- choose(0L, 1L)
      e <- choose(0L, 0x7feL)
      m <- choose(0L, 0xfffffffffffffL)
    } yield java.lang.Double.longBitsToDouble((s << 63) | (e << 52) | m)
  )

  /** Arbitrary instance of Char */
  implicit lazy val arbChar: Arbitrary[Char] = Arbitrary {
    // valid ranges are [0x0000, 0xD7FF] and [0xE000, 0xFFFD].
    //
    // ((0xFFFD + 1) - 0xE000) + ((0xD7FF + 1) - 0x0000) - 1
    choose(0, 63485).map { i =>
      if (i <= 0xD7FF) i.toChar
      else (i + 2048).toChar
    }
  }

  /** Arbitrary instance of Byte */
  implicit lazy val arbByte: Arbitrary[Byte] =
    Arbitrary(Gen.chooseNum(Byte.MinValue, Byte.MaxValue))

  /** Arbitrary instance of Short */
  implicit lazy val arbShort: Arbitrary[Short] =
    Arbitrary(Gen.chooseNum(Short.MinValue, Short.MaxValue))

  /** Absolutely, totally, 100% arbitrarily chosen Unit. */
  implicit lazy val arbUnit: Arbitrary[Unit] =
    Arbitrary(Gen.const(()))

  /**** Arbitrary instances of other common types ****/

  /** Arbitrary instance of String */
  implicit lazy val arbString: Arbitrary[String] =
    Arbitrary(Gen.stringOf(arbitrary[Char]))

  /** Arbitrary instance of Symbol */
  implicit lazy val arbSymbol: Arbitrary[Symbol] =
    Arbitrary(arbitrary[String].map(Symbol(_)))

  /** Arbitrary instance of Date */
  implicit lazy val arbDate: Arbitrary[java.util.Date] =
    Arbitrary(Gen.calendar.map(_.getTime))

  /** Arbitrary instance of Calendar */
  implicit lazy val arbCalendar: Arbitrary[java.util.Calendar] =
    Arbitrary(Gen.calendar)

  /** Arbitrary instance of Throwable */
  implicit lazy val arbThrowable: Arbitrary[Throwable] =
    Arbitrary(oneOf(const(new Exception), const(new Error)))

  /** Arbitrary instance of Exception */
  implicit lazy val arbException: Arbitrary[Exception] =
    Arbitrary(const(new Exception))

  /** Arbitrary instance of Error */
  implicit lazy val arbError: Arbitrary[Error] =
    Arbitrary(const(new Error))

  /** Arbitrary instance of UUID */
  implicit lazy val arbUuid: Arbitrary[java.util.UUID] =
    Arbitrary(Gen.uuid)

  /** Arbitrary BigInt */
  implicit lazy val arbBigInt: Arbitrary[BigInt] = {
    val long: Gen[Long] =
      Gen.choose(Long.MinValue, Long.MaxValue).map(x => if (x == 0) 1L else x)

    val gen1: Gen[BigInt] = for { x <- long } yield BigInt(x)
    val gen2: Gen[BigInt] = for { x <- gen1; y <- long } yield x * y
    val gen3: Gen[BigInt] = for { x <- gen2; y <- long } yield x * y
    val gen4: Gen[BigInt] = for { x <- gen3; y <- long } yield x * y

    val gen0: Gen[BigInt] =
      oneOf(
        BigInt(0),
        BigInt(1),
        BigInt(-1),
        BigInt(Int.MaxValue) + 1,
        BigInt(Int.MinValue) - 1,
        BigInt(Long.MaxValue),
        BigInt(Long.MinValue),
        BigInt(Long.MaxValue) + 1,
        BigInt(Long.MinValue) - 1)

    Arbitrary(frequency((5, gen0), (5, gen1), (4, gen2), (3, gen3), (2, gen4)))
  }

  /** Arbitrary BigDecimal */
  implicit lazy val arbBigDecimal: Arbitrary[BigDecimal] = {
    import java.math.MathContext, MathContext._

    val genMathContext0: Gen[MathContext] =
      oneOf(DECIMAL32, DECIMAL64, DECIMAL128)

    val long: Gen[Long] =
      Gen.choose(Long.MinValue, Long.MaxValue).map(x => if (x == 0) 1L else x)

    val genWholeBigDecimal: Gen[BigDecimal] =
      long.map(BigDecimal(_))

    val genSmallBigDecimal: Gen[BigDecimal] =
      for {
        mc <- genMathContext0
        n <- long
        d <- long
      } yield BigDecimal(n, 0, mc) / d.toDouble

    val genMathContext: Gen[MathContext] =
      oneOf(UNLIMITED, DECIMAL32, DECIMAL64, DECIMAL128)

    val genLargeBigDecimal: Gen[BigDecimal] =
      for {
        mc <- genMathContext
        n <- arbitrary[BigInt]
        scale <- Gen.choose(-300, 300)
      } yield {
        try {
          BigDecimal(n, scale, mc)
        } catch { case _: ArithmeticException =>
          // Handle the case where scale/precision conflict
          BigDecimal(n, scale, UNLIMITED)
        }
      }

    val genSpecificBigDecimal: Gen[BigDecimal] =
      oneOf(
        BigDecimal(0),
        BigDecimal(1),
        BigDecimal(-1),
        BigDecimal("1e-300"),
        BigDecimal("-1e-300"))

    Arbitrary(frequency(
      (5, genWholeBigDecimal),
      (10, genSmallBigDecimal),
      (10, genLargeBigDecimal),
      (5, genSpecificBigDecimal)))
  }

  /** Arbitrary java.lang.Number */
  implicit lazy val arbNumber: Arbitrary[Number] = {
    val gen = Gen.oneOf(
      arbitrary[Byte], arbitrary[Short], arbitrary[Int], arbitrary[Long],
      arbitrary[Float], arbitrary[Double]
    )
    Arbitrary(gen.map(_.asInstanceOf[Number]))
    // XXX TODO - restore BigInt and BigDecimal
    // Arbitrary(oneOf(arbBigInt.arbitrary :: (arbs map (_.arbitrary) map toNumber) : _*))
  }

  /** Arbitrary instance of FiniteDuration */
  implicit lazy val arbFiniteDuration: Arbitrary[FiniteDuration] =
    Arbitrary(Gen.finiteDuration)

  /**
   * Arbitrary instance of Duration.
   *
   * In addition to `FiniteDuration` values, this can generate `Duration.Inf`,
   * `Duration.MinusInf`, and `Duration.Undefined`.
   */
  implicit lazy val arbDuration: Arbitrary[Duration] =
    Arbitrary(Gen.duration)

  /** Generates an arbitrary property */
  implicit lazy val arbProp: Arbitrary[Prop] = {
    import Prop._
    val undecidedOrPassed = forAll { (b: Boolean) =>
      b ==> true
    }
    Arbitrary(frequency(
      (4, falsified),
      (4, passed),
      (3, proved),
      (3, undecidedOrPassed),
      (2, undecided),
      (1, exception(null))
    ))
  }

  /** Arbitrary instance of test parameters */
  implicit lazy val arbTestParameters: Arbitrary[Test.Parameters] =
    Arbitrary(for {
      _minSuccTests <- choose(10,200)
      _maxDiscardRatio <- choose(0.2f,10f)
      _minSize <- choose(0,500)
      sizeDiff <- choose(0,500)
      _maxSize <- choose(_minSize, _minSize + sizeDiff)
      _workers <- choose(1,4)
    } yield Test.Parameters.default
        .withMinSuccessfulTests(_minSuccTests)
        .withMaxDiscardRatio(_maxDiscardRatio)
        .withMinSize(_minSize)
        .withMaxSize(_maxSize)
        .withWorkers(_workers)
    )

  /** Arbitrary instance of gen params */
  implicit lazy val arbGenParams: Arbitrary[Gen.Parameters] =
    Arbitrary(for {
      sz <- arbitrary[Int].suchThat(_ >= 0)
    } yield Gen.Parameters.default.withSize(sz))


  // Specialised collections //

  /** Arbitrary instance of scala.collection.BitSet */
  implicit lazy val arbBitSet: Arbitrary[collection.BitSet] = Arbitrary(
    buildableOf[collection.BitSet,Int](sized(sz => choose(0,sz)))
  )


  // Higher-order types //

  /** Arbitrary instance of [[org.scalacheck.Gen]] */
  implicit def arbGen[T](implicit a: Arbitrary[T]): Arbitrary[Gen[T]] =
    Arbitrary(frequency(
      (5, arbitrary[T].map(const(_))),
      (1, Gen.fail)
    ))

  /** Arbitrary instance of the Option type */
  implicit def arbOption[T](implicit a: Arbitrary[T]): Arbitrary[Option[T]] =
    Arbitrary(Gen.option(a.arbitrary))

  /** Arbitrary instance of the Either type */
  implicit def arbEither[T, U](implicit at: Arbitrary[T], au: Arbitrary[U]): Arbitrary[Either[T, U]] =
    Arbitrary(Gen.either(at.arbitrary, au.arbitrary))

  /** Arbitrary instance of the Future type */
  implicit def arbFuture[T](implicit a: Arbitrary[T]): Arbitrary[Future[T]] =
    Arbitrary(Gen.oneOf(arbitrary[T].map(Future.successful), arbitrary[Throwable].map(Future.failed)))

  /** Arbitrary instance of the Try type */
  implicit def arbTry[T](implicit a: Arbitrary[T]): Arbitrary[Try[T]] =
    Arbitrary(Gen.oneOf(arbitrary[T].map(Success(_)), arbitrary[Throwable].map(Failure(_))))

  /** Arbitrary instance of any [[org.scalacheck.util.Buildable]] container
   *  (such as lists, arrays, streams / lazy lists, etc). The maximum size of the container
   *  depends on the size generation parameter. */
  implicit def arbContainer[C[_],T](implicit
    a: Arbitrary[T], b: Buildable[T,C[T]], t: C[T] => Traversable[T]
  ): Arbitrary[C[T]] = Arbitrary(buildableOf[C[T],T](arbitrary[T]))

  /** Arbitrary instance of any [[org.scalacheck.util.Buildable]] container
   *  (such as maps). The maximum size of the container depends on the size
   *  generation parameter. */
  implicit def arbContainer2[C[_,_],T,U](implicit
    a: Arbitrary[(T,U)], b: Buildable[(T,U),C[T,U]], t: C[T,U] => Traversable[(T,U)]
  ): Arbitrary[C[T,U]] = Arbitrary(buildableOf[C[T,U],(T,U)](arbitrary[(T,U)]))

  implicit def arbEnum[A <: java.lang.Enum[A]](implicit A: reflect.ClassTag[A]): Arbitrary[A] = {
    val values = A.runtimeClass.getEnumConstants.asInstanceOf[Array[A]]
    Arbitrary(Gen.oneOf(values.toIndexedSeq))
  }

  implicit def arbPartialFunction[A: Cogen, B: Arbitrary]: Arbitrary[PartialFunction[A, B]] =
    Arbitrary(implicitly[Arbitrary[A => Option[B]]].arbitrary.map(Function.unlift))
}
