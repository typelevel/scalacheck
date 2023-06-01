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

import java.time._
import java.time.temporal.ChronoUnit
import java.util.Date
import scala.util.{Try, Success, Failure}

object ChooseSpecification extends Properties("Choose") with time.OrderingVersionSpecific {

  def chooseProp[A](implicit C: Choose[A], A: Arbitrary[A], O: Ordering[A]): Prop =
    Prop.forAll { (l: A, h: A) =>
      checkChoose(l, h)
    }

  def checkChoose[A](l: A, h: A)(implicit C: Choose[A], O: Ordering[A]): Prop = {
    import O.mkOrderingOps
    Try(choose(l, h)) match {
      case Success(g) => Prop.forAll(g) { x => l <= x && x <= h }
      case Failure(_: Choose.IllegalBoundsError[_]) => l > h
      case Failure(e) => throw e
    }
  }

  property("choose[Duration]") = chooseProp[Duration]

  property("choose[Instant]") = chooseProp[Instant]

  property("choose[Month]") = chooseProp[Month]

  property("choose[Year]") = chooseProp[Year]

  property("choose[LocalTime]") = chooseProp[LocalTime]

  property("choose[LocalDate]") = chooseProp[LocalDate]

  property("choose[LocalDateTime]") = chooseProp[LocalDateTime]

  property("choose[MonthDay]") = chooseProp[MonthDay]

  property("choose[ZoneOffset]") = chooseProp[ZoneOffset]

  property("choose[OffsetTime]") = chooseProp[OffsetTime]

  property("choose[OffsetDateTime]") = chooseProp[OffsetDateTime]

  property("choose[YearMonth]") = chooseProp[YearMonth]

  property("choose[ZonedDateTime]") = chooseProp[ZonedDateTime]

  /** Generate a duration which is at least 1 second smaller than the max
    * duration the type can support. We use this to avoid the incredibly
    * unlikely event of overflowing in the handle-min-nanos-duration test.
    */
  lazy val genOneSecondLessThanMaxDuration: Gen[Duration] =
    Gen.choose(Duration.ofSeconds(Long.MinValue), Duration.ofSeconds(Long.MaxValue - 1L, 999999999L))

  // https://github.com/typelevel/scalacheck/issues/762
  property("choose[Duration](min, max.withNanos") =
    Prop.forAllNoShrink(genOneSecondLessThanMaxDuration) { (min: Duration) =>
      // At most one second larger, with 0 in nanos.
      val max: Duration =
        min.plusSeconds(1L).withNanos(0)

      // We either select the min second value or the max second value. In the
      // case where we select the max second value the valid nano range is [0,0]
      // (was incorrectly [1,0] in the past which caused an exception).
      checkChoose(min, max)
    }

  /** Generate an Instant which is at least 1 second smaller than the max
    * Instant the type can support. We use this to avoid the incredibly
    * unlikely event of overflowing in the handle-min-nanos-instant test.
    */
  lazy val genOneSecondLessThanMaxInstant: Gen[Instant] =
    Gen.choose(Instant.MIN, Instant.MAX.minusSeconds(1L))

  // https://github.com/typelevel/scalacheck/issues/762
  property("choose[Instant](min, max.withNanos)") =
    Prop.forAllNoShrink(genOneSecondLessThanMaxInstant) { (min: Instant) =>
      // At most one second later, with 0 in nanos.
      val max: Instant =
        min.plusSeconds(1L).truncatedTo(ChronoUnit.NANOS)

      // We either select the min second value or the max second value. In the
      // case where we select the max second value the valid nano range is [0,0]
      // (was incorrectly [1,0] in the past which caused an exception).
      checkChoose(min, max)
    }

  property("choose[Int]") =
    Prop.forAll { (l: Int, h: Int) =>
      checkChoose(l, h)
    }

  property("choose[Long]") =
    Prop.forAll { (l: Long, h: Long) =>
      checkChoose(l, h)
    }

  property("choose[Double]") =
    Prop.forAll { (l: Double, h: Double) =>
      checkChoose(l, h)
    }

  import Double.{MinValue, MaxValue}
  property("choose(MinValue, MaxValue)") =
    Prop.forAll(choose(MinValue, MaxValue)) { x =>
      MinValue <= x && x <= MaxValue && !x.isNaN
    }

  import Double.{NegativeInfinity, PositiveInfinity}
  property("choose(NegativeInfinity, PositiveInfinity)") =
    Prop.forAll(Gen.choose(NegativeInfinity, PositiveInfinity)) { x =>
      NegativeInfinity <= x && x <= PositiveInfinity && !x.isNaN
    }

  property("choose(NegativeInfinity, PositiveInfinity)") =
    Prop.forAllNoShrink(listOfN(3, choose(NegativeInfinity, PositiveInfinity))) { xs =>
      xs.exists(_ != 0d)
    }

  val manualBigInt: Gen[BigInt] =
    nonEmptyContainerOf[Array, Byte](Arbitrary.arbitrary[Byte]).map(BigInt(_))

  property("choose[BigInt]") =
    Prop.forAll(manualBigInt, manualBigInt) { (l: BigInt, h: BigInt) =>
      checkChoose(l, h)
    }

  property("choose[BigInteger]") =
    Prop.forAll(manualBigInt, manualBigInt) { (x0: BigInt, y0: BigInt) =>
      val (x, y) = (x0.bigInteger, y0.bigInteger)
      checkChoose(x, y)
    }

  property("choose(BigInt( 2^(2^18 - 1)), BigInt(-2^(2^18 - 1)))") = {
    val (l, h) = (BigInt(-2).pow(262143), BigInt(2).pow(262143))
    checkChoose(l, h)
  }

  property("choose[BigDecimal]") =
    Prop.forAll { (x0: Double, y0: Double) =>
      val (x, y) = (BigDecimal(x0), BigDecimal(y0))
      checkChoose(x, y)
    }

  property("choose[BigDecimal]") =
    Prop.forAll { (x0: Double, y0: Double) =>
      val (x, y) = (BigDecimal(x0).bigDecimal, BigDecimal(y0).bigDecimal)
      checkChoose(x, y)
    }

  property("xmap[Long,Date]") = {
    implicit val chooseDate: Choose[Date] =
      Choose.xmap[Long, Date](new Date(_), _.getTime)
    Prop.forAll { (l: Date, h: Date) =>
      checkChoose(l, h)
    }
  }
}
