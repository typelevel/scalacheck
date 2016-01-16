/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2015 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import java.util.Calendar

import language.higherKinds
import concurrent.Future
import scala.util.{Failure, Success, Try}

import util.{FreqMap, Buildable}


sealed abstract class Arbitrary[T] {
  val arbitrary: Gen[T]
}

/** Defines implicit [[org.scalacheck.Arbitrary]] instances for common types.
 *  <p>
 *  ScalaCheck
 *  uses implicit [[org.scalacheck.Arbitrary]] instances when creating properties
 *  out of functions with the `Prop.property` method, and when
 *  the `Arbitrary.arbitrary` method is used. For example, the
 *  following code requires that there exists an implicit
 *  `Arbitrary[MyClass]` instance:
 *  </p>
 *
 *  {{{
 *    val myProp = Prop.forAll { myClass: MyClass =>
 *      ...
 *    }
 *
 *    val myGen = Arbitrary.arbitrary[MyClass]
 *  }}}
 *
 *  <p>
 *  The required implicit definition could look like this:
 *  </p>
 *
 *  {{{
 *    implicit val arbMyClass: Arbitrary[MyClass] = Arbitrary(...)
 *  }}}
 *
 *  <p>
 *  The factory method `Arbitrary(...)` takes a generator of type
 *  `Gen[T]` and returns an instance of `Arbitrary[T]`.
 *  </p>
 *
 *  <p>
 *  The `Arbitrary` module defines implicit [[org.scalacheck.Arbitrary]]
 *  instances for common types, for convenient use in your properties and
 *  generators.
 *  </p>
 */
object Arbitrary extends ArbitraryLowPriority with ArbitraryArities

/** separate trait to have same priority as ArbitraryArities */
private[scalacheck] sealed trait ArbitraryLowPriority {
  import Gen.{const, choose, sized, frequency, oneOf, buildableOf, resize}
  import collection.{immutable, mutable}
  import java.util.Date

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
  implicit lazy val arbChar: Arbitrary[Char] = Arbitrary(
    Gen.frequency(
      (0xD800-Char.MinValue, Gen.choose[Char](Char.MinValue,0xD800-1)),
      (Char.MaxValue-0xDFFF, Gen.choose[Char](0xDFFF+1,Char.MaxValue))
    )
  )

  /** Arbitrary instance of Byte */
  implicit lazy val arbByte: Arbitrary[Byte] = Arbitrary(
    Gen.chooseNum(Byte.MinValue, Byte.MaxValue)
  )

  /** Arbitrary instance of Short */
  implicit lazy val arbShort: Arbitrary[Short] = Arbitrary(
    Gen.chooseNum(Short.MinValue, Short.MaxValue)
  )

  /** Absolutely, totally, 100% arbitrarily chosen Unit. */
  implicit lazy val arbUnit: Arbitrary[Unit] = Arbitrary(const(()))

  /**** Arbitrary instances of other common types ****/

  /** Arbitrary instance of String */
  implicit lazy val arbString: Arbitrary[String] =
    Arbitrary(arbitrary[List[Char]] map (_.mkString))

  /** Arbitrary instance of Date */
  implicit lazy val arbDate: Arbitrary[Date] = Arbitrary(Gen.CalendarGen.default.map(_.getTime))

  /** Arbirtrary instance of Calendar */
  implicit lazy val arbCalendar: Arbitrary[Calendar] = Arbitrary(Gen.CalendarGen.default)

  /** Arbitrary instance of Throwable */
  implicit lazy val arbThrowable: Arbitrary[Throwable] =
    Arbitrary(oneOf(const(new Exception), const(new Error)))

  /** Arbitrary instance of Exception */
  implicit lazy val arbException: Arbitrary[Exception] =
    Arbitrary(const(new Exception))

  /** Arbitrary instance of Error */
  implicit lazy val arbError: Arbitrary[Error] =
    Arbitrary(const(new Error))

  /** Arbitrary BigInt */
  implicit lazy val arbBigInt: Arbitrary[BigInt] = {
    def chooseBigInt: Gen[BigInt] =
      sized((s: Int) => choose(-s, s)) map (x => BigInt(x))

    def chooseReallyBigInt: Gen[BigInt] = for {
      bi <- chooseBigInt
      n <- choose(32,128)
    } yield bi << n

    Arbitrary(
      frequency(
        (5, chooseBigInt),
        (10, chooseReallyBigInt),
        (1, BigInt(0)),
        (1, BigInt(1)),
        (1, BigInt(-1)),
        (1, BigInt(Int.MaxValue) + 1),
        (1, BigInt(Int.MinValue) - 1),
        (1, BigInt(Long.MaxValue)),
        (1, BigInt(Long.MinValue)),
        (1, BigInt(Long.MaxValue) + 1),
        (1, BigInt(Long.MinValue) - 1)
      )
    )
  }

  /** Arbitrary BigDecimal */
  implicit lazy val arbBigDecimal: Arbitrary[BigDecimal] = {
    import java.math.MathContext._
    val mcGen = oneOf(UNLIMITED, DECIMAL32, DECIMAL64, DECIMAL128)
    val bdGen = for {
      x <- arbBigInt.arbitrary
      mc <- mcGen
      limit <- const(if(mc == UNLIMITED) 0 else math.max(x.abs.toString.length - mc.getPrecision, 0))
      scale <- Gen.chooseNum(Int.MinValue + limit , Int.MaxValue)
    } yield {
      try {
        BigDecimal(x, scale, mc)
      } catch {
        case ae: java.lang.ArithmeticException => BigDecimal(x, scale, UNLIMITED) // Handle the case where scale/precision conflict
      }
    }
    Arbitrary(bdGen)
  }

  /** Arbitrary java.lang.Number */
  implicit lazy val arbNumber: Arbitrary[Number] = {
    val gen = Gen.oneOf(
      arbitrary[Byte], arbitrary[Short], arbitrary[Int], arbitrary[Long],
      arbitrary[Float], arbitrary[Double]
    )
    Arbitrary(gen map (_.asInstanceOf[Number]))
    // XXX TODO - restore BigInt and BigDecimal
    // Arbitrary(oneOf(arbBigInt.arbitrary :: (arbs map (_.arbitrary) map toNumber) : _*))
  }

  /** Generates an arbitrary property */
  implicit lazy val arbProp: Arbitrary[Prop] = {
    import Prop._
    val undecidedOrPassed = forAll { b: Boolean =>
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
      sz <- arbitrary[Int] suchThat (_ >= 0)
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
      (5, arbitrary[T] map (const(_))),
      (1, Gen.fail)
    ))

  /** Arbitrary instance of the Option type */
  implicit def arbOption[T](implicit a: Arbitrary[T]): Arbitrary[Option[T]] =
    Arbitrary(sized(n =>
      // When n is larger, make it less likely that we generate None,
      // but still do it some of the time. When n is zero, we always
      // generate None, since it's the smallest value.
      frequency(
        (n, resize(n / 2, arbitrary[T]).map(Some(_))),
        (1, const(None)))))

  /** Arbitrary instance of the Either type */
  implicit def arbEither[T, U](implicit at: Arbitrary[T], au: Arbitrary[U]): Arbitrary[Either[T, U]] =
    Arbitrary(oneOf(arbitrary[T].map(Left(_)), arbitrary[U].map(Right(_))))

  /** Arbitrary instance of the Future type */
  implicit def arbFuture[T](implicit a: Arbitrary[T]): Arbitrary[Future[T]] =
    Arbitrary(Gen.oneOf(arbitrary[T].map(Future.successful), arbitrary[Throwable].map(Future.failed)))

  /** Arbitrary instance of the Try type */
  implicit def arbTry[T](implicit a: Arbitrary[T]): Arbitrary[Try[T]] =
    Arbitrary(Gen.oneOf(arbitrary[T].map(Success(_)), arbitrary[Throwable].map(Failure(_))))

  /** Arbitrary instance of any [[org.scalacheck.util.Buildable]] container
   *  (such as lists, arrays, streams, etc). The maximum size of the container
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
    Arbitrary(Gen.oneOf(values))
  }
}
