package org.scalacheck.time

import java.time._
import java.time.chrono._

/** On Scala <= 2.12 it is used to help the compiler figure out some `Ordering`
  * instances needed for testing.
  */
private[time] trait OrphanInstances {

  implicit final lazy val localDateOrdering: Ordering[LocalDate] =
    Ordering.by((ld: LocalDate) => (ld: ChronoLocalDate))

  implicit final lazy val localDateTimeOrdering: Ordering[LocalDateTime] =
    Ordering.by((ldt: LocalDateTime) => (ldt: ChronoLocalDateTime[_]))

  implicit final lazy val zonedDateTimeOrdering: Ordering[ZonedDateTime] =
    Ordering.by((zdt: ZonedDateTime) => (zdt: ChronoZonedDateTime[_]))
}
