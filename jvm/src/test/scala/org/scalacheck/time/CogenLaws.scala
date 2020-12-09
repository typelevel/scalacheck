package org.scalacheck.time

import java.time._
import org.scalacheck.Gen._
import org.scalacheck.Prop._
import org.scalacheck.Shrink._
import org.scalacheck._
import scala.util._

object CogenLaws extends Properties("java.time CogenLaws") {
  import CogenSpecification._

  include(cogenLaws[Duration], "cogenDuration")
  include(cogenLaws[Instant], "cogenInstant")
  include(cogenLaws[Month], "cogenMonth")
  include(cogenLaws[Year], "cogenYear")
  include(cogenLaws[LocalTime], "cogenLocalTime")
  include(cogenLaws[LocalDate], "cogenLocalDate")
  include(cogenLaws[LocalDateTime], "cogenLocalDateTime")
  include(cogenLaws[MonthDay], "cogenMonthDay")
  include(cogenLaws[ZoneOffset], "cogenZoneOffset")
  include(cogenLaws[OffsetTime], "cogenOffsetTime")
  include(cogenLaws[OffsetDateTime], "cogenOffsetDateTime")
  include(cogenLaws[YearMonth], "cogenYearMonth")
  include(cogenLaws[ZonedDateTime], "cogenZonedDateTime")
  include(cogenLaws[ZoneId], "cogenZoneId")
  include(cogenLaws[Period], "cogenPeriod")
}
