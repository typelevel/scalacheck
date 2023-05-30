/*
 * ScalaCheck                                                           
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.        
 * http://www.scalacheck.org                                            
 *                                                                      
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.        
 */

package org.scalacheck.time

import org.scalacheck._
import java.time._
import java.time.temporal._

/** [[Cogen]] instances for `java.time` types. */
private[scalacheck] trait JavaTimeCogen {

  // ChronoUnit

  implicit final lazy val cogenChronoUnit: Cogen[ChronoUnit] =
    Cogen[Int].contramap(_.ordinal)

  // Duration

  implicit final lazy val cogenJavaDuration: Cogen[Duration] =
    Cogen[(Long, Int)].contramap(value => (value.getSeconds, value.getNano))

  // Instant

  implicit final lazy val cogenInstant: Cogen[Instant] =
    Cogen[(Long, Int)].contramap(value => (value.getEpochSecond, value.getNano))

  // Month

  implicit final lazy val cogenMonth: Cogen[Month] =
    Cogen[Int].contramap(_.ordinal)

  // Year

  implicit final lazy val cogenYear: Cogen[Year] =
    Cogen[Int].contramap(_.getValue)

  // LocalDate

  implicit final lazy val cogenLocalDate: Cogen[LocalDate] =
    Cogen[(Int, Int, Int)].contramap(value => (value.getYear, value.getMonthValue, value.getDayOfMonth))

  // LocalTime

  implicit final lazy val cogenLocalTime: Cogen[LocalTime] =
    Cogen[Long].contramap(_.toNanoOfDay)

  // LocalDateTime

  implicit final lazy val cogenLocalDateTime: Cogen[LocalDateTime] =
    Cogen[(LocalDate, LocalTime)].contramap(value => (value.toLocalDate, value.toLocalTime))

  // MonthDay

  implicit final lazy val cogenMonthDay: Cogen[MonthDay] =
    Cogen[(Month, Int)].contramap(value => (value.getMonth, value.getDayOfMonth))

  // ZoneOffset

  implicit final lazy val cogenZoneOffset: Cogen[ZoneOffset] =
    Cogen[Int].contramap(_.getTotalSeconds)

  // ZoneId

  implicit final lazy val cogenZoneId: Cogen[ZoneId] =
    Cogen[String].contramap(_.toString) // This may seem contrived, and in a
                                        // way it is, but ZoneId values
                                        // _without_ offsets are basically
                                        // just newtypes of String.

  // OffsetTime

  implicit final lazy val cogenOffsetTime: Cogen[OffsetTime] =
    Cogen[(LocalTime, ZoneOffset)].contramap(value => (value.toLocalTime, value.getOffset))

  // OffsetDateTime

  implicit final lazy val cogenOffsetDateTime: Cogen[OffsetDateTime] =
    Cogen[(LocalDateTime, ZoneOffset)].contramap(value => (value.toLocalDateTime, value.getOffset))

  // Period

  implicit final lazy val cogenPeriod: Cogen[Period] =
    Cogen[(Int, Int, Int)].contramap(value => (value.getYears, value.getMonths, value.getDays))

  // YearMonth

  implicit final lazy val cogenYearMonth: Cogen[YearMonth] =
    Cogen[(Int, Month)].contramap(value => (value.getYear, value.getMonth))

  // ZonedDateTime

  implicit final lazy val cogenZonedDateTime: Cogen[ZonedDateTime] =
    Cogen[(LocalDateTime, ZoneOffset)].contramap(value => (value.toLocalDateTime, value.getOffset))

  // DayOfWeek

  implicit final lazy val cogenDayOfWeek: Cogen[DayOfWeek] =
    Cogen[Int].contramap(_.ordinal)

  // temporal.ChronoField

  implicit final lazy val cogenChronoField: Cogen[ChronoField] =
    Cogen[Int].contramap(_.ordinal)
}
