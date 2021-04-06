/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import Gen._

import java.time._
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
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
        case Failure(_) => l > h
    }
  }

  property("choose-duration") = chooseProp[Duration]

  property("choose-instant") = chooseProp[Instant]

  property("choose-month") = chooseProp[Month]

  property("choose-year") = chooseProp[Year]

  property("choose-localTime") = chooseProp[LocalTime]

  property("choose-localDate") = chooseProp[LocalDate]

  property("choose-localDateTime") = chooseProp[LocalDateTime]

  property("choose-monthDay") = chooseProp[MonthDay]

  property("choose-zoneOffset") = chooseProp[ZoneOffset]

  property("choose-offsetTime") = chooseProp[OffsetTime]

  property("choose-offsetDateTime") = chooseProp[OffsetDateTime]

  property("choose-yearMonth") = chooseProp[YearMonth]

  property("choose-zonedDateTime") = chooseProp[ZonedDateTime]

  /** Generate a duration which is at least 1 second smaller than the max
    * duration the type can support. We use this to avoid the incredibly
    * unlikely event of overflowing in the handle-min-nanos-duration test.
    */
  lazy val genOneSecondLessThanMaxDuration: Gen[Duration] =
    Gen.choose(Duration.ofSeconds(Long.MinValue), Duration.ofSeconds(Long.MaxValue - 1L, 999999999L))

  // https://github.com/typelevel/scalacheck/issues/762
  property("handle-min-nanos-duration") = Prop.forAllNoShrink(genOneSecondLessThanMaxDuration){(min: Duration) =>
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
  property("handle-min-nanos-instant") = Prop.forAllNoShrink(genOneSecondLessThanMaxInstant){(min: Instant) =>
    // At most one second later, with 0 in nanos.
    val max: Instant =
      min.plusSeconds(1L).truncatedTo(ChronoUnit.NANOS)

    // We either select the min second value or the max second value. In the
    // case where we select the max second value the valid nano range is [0,0]
    // (was incorrectly [1,0] in the past which caused an exception).
    checkChoose(min, max)
  }

  property("choose-int") = Prop.forAll { (l: Int, h: Int) =>
    Try(choose(l, h)) match {
      case Success(g) => Prop.forAll(g) { x => l <= x && x <= h }
      case Failure(_) => Prop(l > h)
    }
  }

  property("choose-long") = Prop.forAll { (l: Long, h: Long) =>
    Try(choose(l, h)) match {
      case Success(g) => Prop.forAll(g) { x => l <= x && x <= h }
      case Failure(_) => Prop(l > h)
    }
  }

  property("choose-double") = Prop.forAll { (l: Double, h: Double) =>
    Try(choose(l, h)) match {
      case Success(g) => Prop.forAll(g) { x => l <= x && x <= h }
      case Failure(_) => Prop(l > h)
    }
  }

  import Double.{MinValue, MaxValue}
  property("choose-large-double") = Prop.forAll(choose(MinValue, MaxValue)) { x =>
    MinValue <= x && x <= MaxValue && !x.isNaN
  }

  import Double.{NegativeInfinity, PositiveInfinity}
  property("choose-infinite-double") = {
    Prop.forAll(Gen.choose(NegativeInfinity, PositiveInfinity)) { x =>
      NegativeInfinity <= x && x <= PositiveInfinity && !x.isNaN
    }
  }

  property("choose-infinite-double-fix-zero-defect-379") = {
    Prop.forAllNoShrink(listOfN(3, choose(NegativeInfinity, PositiveInfinity))) { xs =>
      xs.exists(_ != 0d)
    }
  }

  val manualBigInt: Gen[BigInt] =
    nonEmptyContainerOf[Array, Byte](Arbitrary.arbitrary[Byte]).map(BigInt(_))

  property("choose-big-int") =
    Prop.forAll(manualBigInt, manualBigInt) { (l: BigInt, h: BigInt) =>
      Try(choose(l, h)) match {
        case Success(g) => Prop.forAll(g) { x => l <= x && x <= h }
        case Failure(e: Choose.IllegalBoundsError[_]) => Prop(l > h)
        case Failure(e) => throw e
      }
    }

  property("choose-java-big-int") =
    Prop.forAll(manualBigInt, manualBigInt) { (x0: BigInt, y0: BigInt) =>
      val (x, y) = (x0.bigInteger, y0.bigInteger)
      Try(choose(x, y)) match {
        case Success(g) => Prop.forAll(g) { n => x.compareTo(n) <= 0 && y.compareTo(n) >= 0 }
        case Failure(e: Choose.IllegalBoundsError[_]) => Prop(x.compareTo(y) > 0)
        case Failure(e) => throw e
      }
    }

  property("Gen.choose(BigInt( 2^(2^18 - 1)), BigInt(-2^(2^18 - 1)))") = {
    val (l, h) = (BigInt(-2).pow(262143), BigInt(2).pow(262143))
    Prop.forAllNoShrink(Gen.choose(l, h)) { x =>
      l <= x && x <= h
    }
  }

  property("choose-big-decimal") =
    Prop.forAll { (x0: Double, y0: Double) =>
      val (x, y) = (BigDecimal(x0), BigDecimal(y0))
      Try(choose(x, y)) match {
        case Success(g) => Prop.forAll(g) { n => x <= n && n <= y }
        case Failure(e: Choose.IllegalBoundsError[_]) => Prop(x > y)
        case Failure(e) => throw e
      }
    }

  property("choose-java-big-decimal") =
    Prop.forAll { (x0: Double, y0: Double) =>
      val (x, y) = (BigDecimal(x0).bigDecimal, BigDecimal(y0).bigDecimal)
      Try(choose(x, y)) match {
        case Success(g) => Prop.forAll(g) { n => x.compareTo(n) <= 0 && y.compareTo(n) >= 0 }
        case Failure(e: Choose.IllegalBoundsError[_]) => Prop(x.compareTo(y) > 0)
        case Failure(e) => throw e
      }
    }

  property("choose-xmap") = {
    implicit val chooseDate: Choose[Date] =
      Choose.xmap[Long, Date](new Date(_), _.getTime)
    Prop.forAll { (l: Date, h: Date) =>
      Try(choose(l, h)) match {
        case Success(g) => Prop.forAll(g) { x => x.compareTo(l) >= 0 && x.compareTo(h) <= 0 }
        case Failure(_) => Prop(l.after(h))
      }
    }
  }
}
