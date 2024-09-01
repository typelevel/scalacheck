/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package org.scalacheck.time

import org.scalacheck.*

import java.time.*

/** [[Arbitrary]] instances for `java.time` types.
 *
 *  @note
 *    [[Arbitrary]] instances for `java.time` types which are Java enum types are provided by ScalaCheck's general Java
 *    enum support.
 */
private[scalacheck] trait JavaTimeArbitrary {

  // Duration

  // Java duration values are conceptually infinite, thus they do not expose
  // Duration.MAX/Duration.MIN values, but in practice they are finite,
  // restricted by their underlying representation a long and an int.

  implicit final lazy val arbJavaDuration: Arbitrary[Duration] = {
    val minJavaDuration = Duration.ofSeconds(Long.MinValue)
    val maxJavaDuration = Duration.ofSeconds(Long.MaxValue, 999999999L)
    Arbitrary(Gen.choose(minJavaDuration, maxJavaDuration))
  }

  // Instant

  implicit final lazy val arbInstant: Arbitrary[Instant] =
    Arbitrary(Gen.choose(Instant.MIN, Instant.MAX))

  // Year

  implicit final lazy val arbYear: Arbitrary[Year] =
    Arbitrary(Gen.choose(Year.of(Year.MIN_VALUE), Year.of(Year.MAX_VALUE)))

  // LocalDate

  implicit final lazy val arbLocalDate: Arbitrary[LocalDate] =
    Arbitrary(Gen.choose(LocalDate.MIN, LocalDate.MAX))

  // LocalTime

  implicit final lazy val arbLocalTime: Arbitrary[LocalTime] =
    Arbitrary(Gen.choose(LocalTime.MIN, LocalTime.MAX))

  // LocalDateTime

  implicit final lazy val arbLocalDateTime: Arbitrary[LocalDateTime] =
    Arbitrary(Gen.choose(LocalDateTime.MIN, LocalDateTime.MAX))

  // MonthDay

  implicit final lazy val arbMonthDay: Arbitrary[MonthDay] =
    Arbitrary(Gen.choose(MonthDay.of(Month.JANUARY, 1), MonthDay.of(Month.DECEMBER, 31)))

  // ZoneOffset

  implicit final lazy val arbZoneOffset: Arbitrary[ZoneOffset] =
    Arbitrary(
      Gen.oneOf(
        Gen.oneOf(ZoneOffset.MAX, ZoneOffset.MIN, ZoneOffset.UTC),
        Gen.choose(ZoneOffset.MAX, ZoneOffset.MIN) // These look flipped, but they are not.
      )
    )

  // ZoneId

  /** ''Technically'' the available zone ids can change at runtime, so we store an immutable snapshot in time here. We
   *  avoid going through the scala/java collection converters to avoid having to deal with the scala 2.13 changes and
   *  adding a dependency on the collection compatibility library.
   */
  private final lazy val availableZoneIds: Set[ZoneId] =
    ZoneId.getAvailableZoneIds.toArray(Array.empty[String]).toSet.map((value: String) => ZoneId.of(value))

  // ZoneIds by themselves do not describe an offset from UTC (ZoneOffset
  // does), so there isn't a meaningful way to define a choose as they can not
  // be reasonably ordered.

  implicit final lazy val arbZoneId: Arbitrary[ZoneId] =
    Arbitrary(Gen.oneOf(Gen.oneOf(availableZoneIds), arbZoneOffset.arbitrary))

  // OffsetTime

  implicit final lazy val arbOffsetTime: Arbitrary[OffsetTime] =
    Arbitrary(Gen.choose(OffsetTime.MIN, OffsetTime.MAX))

  // OffsetDateTime

  implicit final lazy val arbOffsetDateTime: Arbitrary[OffsetDateTime] =
    Arbitrary(Gen.choose(OffsetDateTime.MIN, OffsetDateTime.MAX))

  // Period

  implicit final lazy val arbPeriod: Arbitrary[Period] =
    Arbitrary(
      for {
        years <- Arbitrary.arbitrary[Int]
        months <- Arbitrary.arbitrary[Int]
        days <- Arbitrary.arbitrary[Int]
      } yield Period.of(years, months, days))

  // YearMonth

  implicit final lazy val arbYearMonth: Arbitrary[YearMonth] =
    Arbitrary(Gen.choose(YearMonth.of(Year.MIN_VALUE, Month.JANUARY), YearMonth.of(Year.MAX_VALUE, Month.DECEMBER)))

  // ZonedDateTime

  implicit final lazy val arbZonedDateTime: Arbitrary[ZonedDateTime] =
    // The ZoneOffset's here look flipped but they are
    // not. ZonedDateTime.of(LocalDateTime.MIN, ZoneOffset.MAX) is _older_
    // than ZonedDateTime.of(LocalDateTime, ZoneOffset.MIN).
    Arbitrary(Gen.choose(
      ZonedDateTime.of(LocalDateTime.MIN, ZoneOffset.MAX),
      ZonedDateTime.of(LocalDateTime.MAX, ZoneOffset.MIN)))
}
