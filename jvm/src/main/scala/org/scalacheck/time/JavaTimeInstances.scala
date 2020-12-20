package org.scalacheck.time

import org.scalacheck._
import java.time._
import java.time.temporal._
import org.scalacheck.Gen.Choose

/** Instances for `java.time` types. */
private[time] trait JavaTimeInstances {

  // temporal.ChronoUnit
  //
  // Arbitrary and Choose instances are already provided by Java enum support.

  implicit final lazy val cogenChronoUnit: Cogen[ChronoUnit] =
    Cogen[Int].contramap(_.ordinal)

  // Duration

  // Java duration values are conceptually infinite, thus they do not expose
  // Duration.MAX/Duration.MIN values, but in practice they are finite,
  // restricted by their underlying representation a long and an int.

  private final lazy val minDuration: Duration =
    Duration.ofSeconds(Long.MinValue)

  private final lazy val maxDuration: Duration =
    Duration.ofSeconds(Long.MaxValue, 999999999L)

  implicit final lazy val chooseDuration: Choose[Duration] =
    new Choose[Duration] {
      override def choose(min: Duration, max: Duration): Gen[Duration] = {
        min.compareTo(max) match {
          case 0 => Gen.const(min)
          case result if result > 0 => Gen.fail
          case _ =>
            val minSeconds: Long = min.getSeconds
            val maxSeconds: Long = max.getSeconds
            Gen.choose(minSeconds, maxSeconds).flatMap{seconds =>
              val minNanos: Int =
                if(seconds == minSeconds) {
                  min.getNano
                } else {
                  1
                }
              val maxNanos: Int =
                if (seconds == maxSeconds) {
                  max.getNano
                } else {
                  999999999
                }
              Gen.choose(minNanos, maxNanos).map(nanos =>
                Duration.ofSeconds(seconds, nanos.toLong)
              )
            }
        }
      }
    }

  implicit final lazy val arbDuration: Arbitrary[Duration] =
    Arbitrary(Gen.choose(minDuration, maxDuration))

  implicit final lazy val cogenDuration: Cogen[Duration] =
    Cogen[(Long, Int)].contramap(value => (value.getSeconds, value.getNano))

  implicit final lazy val shrinkDuration: Shrink[Duration] =
    Shrink[Duration]{value =>
      val q: Duration = value.dividedBy(2)
      if (q == Duration.ZERO) {
        Stream(Duration.ZERO)
      } else {
        q #:: q.negated #:: shrinkDuration.shrink(q)
      }
    }

  // Instant

  implicit final lazy val chooseInstant: Choose[Instant] =
    new Choose[Instant] {
      override def choose(min: Instant, max: Instant): Gen[Instant] =
        min.compareTo(max) match {
          case 0 => Gen.const(min)
          case result if result > 0 => Gen.fail
          case _ =>
            Gen.choose(min.getEpochSecond, max.getEpochSecond).flatMap{epochSecond =>
              val minNano: Int =
                if (epochSecond == min.getEpochSecond) {
                  min.getNano
                } else {
                  1
                }
              val maxNano: Int =
                if (epochSecond == max.getEpochSecond) {
                  max.getNano
                } else {
                  999999999
                }
              Gen.choose(minNano, maxNano).map(nanos =>
                Instant.ofEpochSecond(epochSecond, nanos.toLong)
              )
            }
        }
    }

  implicit final lazy val arbInstant: Arbitrary[Instant] =
    Arbitrary(Gen.choose(Instant.MIN, Instant.MAX))

  implicit final lazy val cogenInstant: Cogen[Instant] =
    Cogen[(Long, Int)].contramap(value => (value.getEpochSecond, value.getNano))

  // Month

  implicit final lazy val chooseMonth: Choose[Month] =
    Choose.xmap[Int, Month](
      value => Month.of((value % 12) + 1),
      _.ordinal
    )

  implicit final lazy val cogenMonth: Cogen[Month] =
    Cogen[Int].contramap(_.ordinal)

  // Year

  implicit final lazy val chooseYear: Choose[Year] =
    new Choose[Year] {
      override def choose(min: Year, max: Year): Gen[Year] =
        min.compareTo(max) match {
          case 0 => Gen.const(min)
          case result if result > 0 => Gen.fail
          case _ =>
            Gen.choose(min.getValue, max.getValue).map(value => Year.of(value))
        }
    }

  implicit final lazy val arbYear: Arbitrary[Year] =
    Arbitrary(Gen.choose(Year.of(Year.MIN_VALUE), Year.of(Year.MIN_VALUE)))

  implicit final lazy val cogenYear: Cogen[Year] =
    Cogen[Int].contramap(_.getValue)

  // LocalDate

  implicit final lazy val chooseLocalDate: Choose[LocalDate] =
    new Choose[LocalDate] {
      override def choose(min: LocalDate, max: LocalDate): Gen[LocalDate] =
        min.compareTo(max) match {
          case 0 => Gen.const(min)
          case result if result > 0 => Gen.fail
          case _ =>
            Gen.choose(min.getYear, max.getYear).flatMap{year =>
              val minMonth: Month =
                if (year == min.getYear) {
                  min.getMonth
                } else {
                  Month.JANUARY
                }
              val maxMonth: Month =
                if (year == max.getYear) {
                  max.getMonth
                } else {
                  Month.DECEMBER
                }
              Gen.choose(minMonth, maxMonth).flatMap{month =>
                val minDay: Int =
                  if (year == min.getYear && month == minMonth) {
                    min.getDayOfMonth
                  } else {
                    1
                  }
                val maxDay: Int =
                  if (year == max.getYear && month == max.getMonth) {
                    max.getDayOfMonth
                  } else {
                    // Calculation is proleptic. Historically inaccurate, but
                    // correct according to ISO-8601.
                    month.length(Year.isLeap(year.toLong))
                  }
                Gen.choose(minDay, maxDay).map(day =>
                  LocalDate.of(year, month, day)
                )
              }
            }
        }
    }

  implicit final lazy val arbLocalDate: Arbitrary[LocalDate] =
    Arbitrary(Gen.choose(LocalDate.MIN, LocalDate.MAX))

  implicit final lazy val cogenLocalDate: Cogen[LocalDate] =
    Cogen[(Int, Int, Int)].contramap(value => (value.getYear, value.getMonthValue, value.getDayOfMonth))

  // LocalTime

  implicit final lazy val chooseLocalTime: Choose[LocalTime] =
    new Choose[LocalTime] {
      def choose(min: LocalTime, max: LocalTime): Gen[LocalTime] =
        Gen.choose(min.toNanoOfDay, max.toNanoOfDay).map(nano =>
          LocalTime.ofNanoOfDay(nano)
        )
    }

  implicit final lazy val arbLocalTime: Arbitrary[LocalTime] =
    Arbitrary(Gen.choose(LocalTime.MIN, LocalTime.MAX))

  implicit final lazy val cogenLocalTime: Cogen[LocalTime] =
    Cogen[Long].contramap(_.toNanoOfDay)

  // LocalDateTime

  implicit final lazy val chooseLocalDateTime: Choose[LocalDateTime] =
    new Choose[LocalDateTime] {
      override def choose(min: LocalDateTime, max: LocalDateTime): Gen[LocalDateTime] = {
        min.compareTo(max) match {
          case 0 => Gen.const(min)
          case result if result > 0 => Gen.fail
          case _ =>
            val minLocalDate: LocalDate =
              min.toLocalDate
            val maxLocalDate: LocalDate =
              max.toLocalDate
            Gen.choose(minLocalDate, maxLocalDate).flatMap{localDate =>
              val minLocalTime: LocalTime =
                if (localDate == minLocalDate) {
                  min.toLocalTime
                } else {
                  LocalTime.MIN
                }
              val maxLocalTime: LocalTime =
                if (localDate == maxLocalDate) {
                  max.toLocalTime
                } else {
                  LocalTime.MAX
                }
              Gen.choose(minLocalTime, maxLocalTime).map(localTime =>
                LocalDateTime.of(localDate, localTime)
              )
            }
        }
      }
    }

  implicit final lazy val arbLocalDateTime: Arbitrary[LocalDateTime] =
    Arbitrary(Gen.choose(LocalDateTime.MIN, LocalDateTime.MAX))

  implicit final lazy val cogenLocalDateTime: Cogen[LocalDateTime] =
    Cogen[(LocalDate, LocalTime)].contramap(value => (value.toLocalDate, value.toLocalTime))

  // MonthDay

  implicit final lazy val chooseMonthDay: Choose[MonthDay] =
    new Choose[MonthDay] {
      override def choose(min: MonthDay, max: MonthDay): Gen[MonthDay] =
        min.compareTo(max) match {
          case 0 => Gen.const(min)
          case result if result > 0 => Gen.fail
          case _ =>
            val minMonth: Month = min.getMonth
            val maxMonth: Month = max.getMonth
            Gen.choose(minMonth, maxMonth).flatMap{month =>
              val minDayOfMonth: Int =
                if (month == minMonth) {
                  min.getDayOfMonth
                } else {
                  1
                }
              val maxDayOfMonth: Int =
                if (month == maxMonth) {
                  max.getDayOfMonth
                } else {
                  month.maxLength
                }
              Gen.choose(minDayOfMonth, maxDayOfMonth).map(dayOfMonth =>
                MonthDay.of(month, dayOfMonth)
              )
            }
        }
    }

  implicit final lazy val arbMonthDay: Arbitrary[MonthDay] =
    Arbitrary(Gen.choose(MonthDay.of(Month.JANUARY, 1), MonthDay.of(Month.DECEMBER, 31)))

  implicit final lazy val cogenMonthDay: Cogen[MonthDay] =
    Cogen[(Month, Int)].contramap(value => (value.getMonth, value.getDayOfMonth))

  // ZoneOffset

  /** ZoneOffset values have some unusual semantics when it comes to
    * ordering. The short explanation is that `(ZoneOffset.MAX <
    * ZoneOffset.MIN) == true`. This is because for any given `LocalDateTime`,
    * that time applied to `ZoneOffset.MAX` will be an older moment in time
    * than that same `LocalDateTime` applied to `ZoneOffset.MIN`.
    *
    * From the JavaDoc,
    *
    * "The offsets are compared in the order that they occur for the same time
    * of day around the world. Thus, an offset of +10:00 comes before an
    * offset of +09:00 and so on down to -18:00."
    *
    * This has the following surprising implication,
    *
    * {{{
    *  scala> ZoneOffset.MIN
    * val res0: java.time.ZoneOffset = -18:00
    *
    * scala> ZoneOffset.MAX
    * val res1: java.time.ZoneOffset = +18:00
    *
    * scala> ZoneOffset.MIN.compareTo(ZoneOffset.MAX)
    * val res3: Int = 129600
    * }}}
    *
    * This implementation is consistent with that comparison.
    *
    * @see [[https://docs.oracle.com/javase/8/docs/api/java/time/ZoneOffset.html#compareTo-java.time.ZoneOffset-]]
    */
  implicit final lazy val chooseZoneOffset: Choose[ZoneOffset] =
    new Choose[ZoneOffset] {
      def choose(min: ZoneOffset, max: ZoneOffset): Gen[ZoneOffset] =
        min.compareTo(max) match {
          case 0 => Gen.const(min)
          case result if result > 0 => Gen.fail
          case _ =>
            // Looks flipped, but it is not.
            Gen.choose(max.getTotalSeconds, min.getTotalSeconds).map(value => ZoneOffset.ofTotalSeconds(value))
        }
    }

  implicit final lazy val arbZoneOffset: Arbitrary[ZoneOffset] =
    Arbitrary(
      Gen.oneOf(
        Gen.oneOf(ZoneOffset.MAX, ZoneOffset.MIN, ZoneOffset.UTC),
        Gen.choose(ZoneOffset.MAX, ZoneOffset.MIN) // These look flipped, but they are not.
      )
    )

  implicit final lazy val cogenZoneOffset: Cogen[ZoneOffset] =
    Cogen[Int].contramap(_.getTotalSeconds)

  // ZoneId

  /** ''Technically'' the available zone ids can change at runtime, so we store
    * an immutable snapshot in time here. We avoid going through the
    * scala/java collection converters to avoid having to deal with the scala
    * 2.13 changes and adding a dependency on the collection compatibility
    * library.
    */
  private final lazy val availableZoneIds: Set[ZoneId] =
    ZoneId.getAvailableZoneIds.toArray(Array.empty[String]).toSet.map((value: String) => ZoneId.of(value))

  // ZoneIds by themselves do not describe an offset from UTC (ZoneOffset
  // does), so there isn't a meaningful way to define a choose as they can not
  // be reasonably ordered.

  implicit final lazy val arbZoneId: Arbitrary[ZoneId] =
    Arbitrary(Gen.oneOf(Gen.oneOf(availableZoneIds), arbZoneOffset.arbitrary))

  implicit final lazy val cogenZoneId: Cogen[ZoneId] =
    Cogen[String].contramap(_.toString) // This may seem contrived, and in a
                                        // way it is, but ZoneId values
                                        // _without_ offsets are basically
                                        // just newtypes of String.

  // OffsetTime

  // This type can be particularly mind bending. Because OffsetTime values
  // have no associated date, and because the Duration between OffsetTime.MIN
  // and OffsetTime.MAX is 59 hours (and change) it can be difficult to write
  // Choose for OffsetTime. One has to be careful to perturb both the
  // LocalTime value and the ZoneOffset in such a way as to not accidentally
  // create an OffsetTime value which is < the min bound or > the max
  // bound. This is the reason that there are more helper functions for this
  // type than others. It is an effort to keep clear what is going on.

  private def secondsUntilOffsetRollover(value: OffsetTime): Int =
    value.getOffset().getTotalSeconds()

  private def shiftForwardByOffset(value: OffsetTime, seconds: Int): OffsetTime =
    value.withOffsetSameLocal(ZoneOffset.ofTotalSeconds(value.getOffset().getTotalSeconds() - seconds))

  private def genShiftOffsetTimeForward(min: OffsetTime, max: OffsetTime, shift: Duration): Gen[OffsetTime] = {
    val shiftSeconds: Int = shift.getSeconds().toInt
    val rolloverSeconds: Int = secondsUntilOffsetRollover(min)
    val lub: Int =
      if (shiftSeconds.compareTo(rolloverSeconds) < 0) {
        shiftSeconds
      } else {
        rolloverSeconds
      }
    Gen.choose(0, lub).flatMap{offsetShift =>
      val shifted: OffsetTime = shiftForwardByOffset(min, offsetShift)
      val localShiftMin: Duration = {
        val durationFromMin: Duration = Duration.between(shifted, min)
        val durationAfterMidnight: Duration = Duration.between(shifted.toLocalTime, LocalTime.MIN)
        // For negative Duration values, larger absolute values are smaller,
        // e.g. Duration.ofHours(-1).compareTo(Duration.ofHours(-2)) > 0. For
        // this calculation we want the Duration with the smallest absolute
        // value, e.g. the one which compares larger.
        if(durationFromMin.compareTo(durationAfterMidnight) > 0) {
          durationFromMin
        } else {
          durationAfterMidnight
        }
      }
      val localShiftMax: Duration = {
        val durationFromMax: Duration = Duration.between(shifted, max)
        val durationFromMidnight: Duration = Duration.between(shifted.toLocalTime, LocalTime.MAX)
        if (durationFromMax.compareTo(durationFromMidnight) < 0) {
          durationFromMax
        } else {
          durationFromMidnight
        }
      }
      Gen.choose(localShiftMin, localShiftMax).map(localShift =>
        shifted.plus(localShift)
      )
    }
  }

  implicit final lazy val chooseOffsetTime: Choose[OffsetTime] =
    new Choose[OffsetTime] {
      override def choose(min: OffsetTime, max: OffsetTime): Gen[OffsetTime] =
        min.compareTo(max) match {
          case 0 => Gen.const(min)
          case result if result > 0 => Gen.fail
          case _ =>
            Gen.choose(Duration.ZERO, Duration.between(min, max)).flatMap{duration =>
              genShiftOffsetTimeForward(min, max, duration)
            }
        }
    }

  implicit final lazy val arbOffsetTime: Arbitrary[OffsetTime] =
    Arbitrary(Gen.choose(OffsetTime.MIN, OffsetTime.MAX))

  implicit final lazy val cogenOffsetTime: Cogen[OffsetTime] =
    Cogen[(LocalTime, ZoneOffset)].contramap(value => (value.toLocalTime, value.getOffset))

  // OffsetDateTime

  implicit final lazy val chooseOffsetDateTime: Choose[OffsetDateTime] =
    new Choose[OffsetDateTime] {
      override def choose(min: OffsetDateTime, max: OffsetDateTime): Gen[OffsetDateTime] =
        min.compareTo(max) match {
          case 0 => Gen.const(min)
          case result if result > 0 => Gen.fail
          case _ =>
            Gen.choose(min.getOffset, max.getOffset).flatMap(offset =>
              Gen.choose(min.toInstant, max.toInstant).map(instant =>
                OffsetDateTime.ofInstant(instant, offset)
              )
            )
        }
    }

  implicit final lazy val arbOffsetDateTime: Arbitrary[OffsetDateTime] =
    Arbitrary(Gen.choose(OffsetDateTime.MIN, OffsetDateTime.MAX))

  implicit final lazy val cogenOffsetDateTime: Cogen[OffsetDateTime] =
    Cogen[(LocalDateTime, ZoneOffset)].contramap(value => (value.toLocalDateTime, value.getOffset))

  // Period

  implicit final lazy val arbPeriod: Arbitrary[Period] =
    Arbitrary(
      for {
        years <- Arbitrary.arbitrary[Int]
        months <- Arbitrary.arbitrary[Int]
        days <- Arbitrary.arbitrary[Int]
      } yield Period.of(years, months, days))

  implicit final lazy val cogenPeriod: Cogen[Period] =
    Cogen[(Int, Int, Int)].contramap(value => (value.getYears, value.getMonths, value.getDays))

  implicit final lazy val shrinkPeriod: Shrink[Period] =
    Shrink.xmap[(Int, Int, Int), Period](
      {
        case (y, m, d) => Period.of(y, m, d)
      },
      value => (value.getYears, value.getMonths, value.getDays)
    )

  // YearMonth

  implicit final lazy val chooseYearMonth: Choose[YearMonth] =
    new Choose[YearMonth] {
      def choose(min: YearMonth, max: YearMonth): Gen[YearMonth] =
        min.compareTo(max) match {
          case 0 => Gen.const(min)
          case result if result > 0 => Gen.fail
          case _ =>
            val minYear: Year = Year.of(min.getYear)
            val maxYear: Year = Year.of(max.getYear)
            Gen.choose(minYear, maxYear).flatMap{year =>
              val minMonth: Month =
                if (minYear == year) {
                  min.getMonth
                } else {
                  Month.JANUARY
                }
              val maxMonth: Month =
                if (maxYear == year) {
                  max.getMonth
                } else {
                  Month.DECEMBER
                }
              Gen.choose(minMonth, maxMonth).map(month =>
                YearMonth.of(year.getValue, month)
              )
            }
        }
    }

  implicit final lazy val arbYearMonth: Arbitrary[YearMonth] =
    Arbitrary(Gen.choose(YearMonth.of(Year.MIN_VALUE, Month.JANUARY), YearMonth.of(Year.MAX_VALUE, Month.DECEMBER)))

  implicit final lazy val cogenYearMonth: Cogen[YearMonth] =
    Cogen[(Int, Month)].contramap(value => (value.getYear, value.getMonth))

  // ZonedDateTime

  implicit final lazy val chooseZonedDateTime: Choose[ZonedDateTime] =
    new Choose[ZonedDateTime] {
      def choose(min: ZonedDateTime, max: ZonedDateTime): Gen[ZonedDateTime] =
        min.compareTo(max) match {
          case 0 => Gen.const(min)
          case result if result > 0 => Gen.fail
          case _ =>
            Gen.choose(min.getOffset, max.getOffset).flatMap(offset =>
              Gen.choose(min.toInstant, max.toInstant).map(instant =>
                ZonedDateTime.ofInstant(instant, offset)
              )
            )
        }
    }

  implicit final lazy val arbZonedDateTime: Arbitrary[ZonedDateTime] =
    // The ZoneOffset's here look flipped by they are
    // not. ZonedDateTime.of(LocalDateTime.MIN, ZoneOffset.MAX) is _older_
    // than ZonedDateTime.of(LocalDateTime, ZoneOffset.MIN).
    Arbitrary(Gen.choose(ZonedDateTime.of(LocalDateTime.MIN, ZoneOffset.MAX), ZonedDateTime.of(LocalDateTime.MAX, ZoneOffset.MIN)))

  implicit final lazy val cogenZonedDateTime: Cogen[ZonedDateTime] =
    Cogen[(LocalDateTime, ZoneOffset)].contramap(value => (value.toLocalDateTime, value.getOffset))

  // DayOfWeek

  implicit final lazy val cogenDayOfWeek: Cogen[DayOfWeek] =
    Cogen[Int].contramap(_.ordinal)

  // temporal.ChronoField

  implicit final lazy val cogenChronoField: Cogen[ChronoField] =
    Cogen[Int].contramap(_.ordinal)
}
