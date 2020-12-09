package org.scalacheck.time

import java.time._
import org.scalacheck.Gen._
import org.scalacheck.Prop._
import org.scalacheck.Shrink._
import org.scalacheck._
import scala.util._

object GenSpecification extends Properties("java.time Gen"){

  private[this] def chooseProp[A](implicit C: Choose[A], A: Arbitrary[A], O: Ordering[A]): Prop = {
    import O.mkOrderingOps
    forAll { (l: A, h: A) =>
      Try(choose(l, h)) match {
        case Success(g) => forAll(g) { x => l <= x && x <= h }
        case Failure(_) => Prop(l > h)
      }
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
}
