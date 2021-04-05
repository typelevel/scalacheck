package org.scalacheck.time

import java.time._
import org.scalacheck.Gen._
import org.scalacheck.Prop._
import org.scalacheck.Shrink._
import org.scalacheck._
import scala.util._
import java.time.temporal.TemporalUnit
import java.time.temporal.ChronoUnit

object GenSpecification extends Properties("java.time Gen") with OrphanInstances {

  private[this] def chooseProp[A](implicit C: Choose[A], A: Arbitrary[A], O: Ordering[A]): Prop =
    forAll { (l: A, h: A) =>
      checkChoose(l, h)
    }

  private[this] def checkChoose[A](l: A, h: A)(implicit C: Choose[A], O: Ordering[A]): Prop = {
    import O.mkOrderingOps
    Try(choose(l, h)) match {
        case Success(g) => forAll(g) { x => l <= x && x <= h }
        case Failure(_) => Prop(l > h)
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
  private[this] lazy val genOneSecondLessThanMaxDuration: Gen[Duration] =
    Gen.choose(Duration.ofSeconds(Long.MinValue), Duration.ofSeconds(Long.MaxValue - 1L, 999999999L))

  // https://github.com/typelevel/scalacheck/issues/762
  property("handle-min-nanos-duration") = forAllNoShrink(genOneSecondLessThanMaxDuration){(min: Duration) =>
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
  private[this] lazy val genOneSecondLessThanMaxInstant: Gen[Instant] =
    Gen.choose(Instant.MIN, Instant.MAX.minusSeconds(1L))

  // https://github.com/typelevel/scalacheck/issues/762
  property("handle-min-nanos-instant") = forAllNoShrink(genOneSecondLessThanMaxInstant){(min: Instant) =>
    // At most one second later, with 0 in nanos.
    val max: Instant =
      min.plusSeconds(1L).truncatedTo(ChronoUnit.NANOS)

    // We either select the min second value or the max second value. In the
    // case where we select the max second value the valid nano range is [0,0]
    // (was incorrectly [1,0] in the past which caused an exception).
    checkChoose(min, max)
  }
}
