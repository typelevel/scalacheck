package org.scalacheck.time

import org.scalacheck._
import java.time._
import org.scalacheck.Gen.Choose

/** [[Gen#Choose]] instances for `java.time` types.
  *
  * @note [[Gen#Choose]] instances for `java.time` types which are Java enum
  *       types are provided by ScalaCheck's general Java enum support.
  */
private[scalacheck] trait JavaTimeChoose {

  // Duration

  implicit final lazy val chooseJavaDuration: Choose[Duration] =
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

  // Month

  implicit final lazy val chooseMonth: Choose[Month] =
    Choose.xmap[Int, Month](
      value => Month.of((value % 12) + 1),
      _.ordinal
    )

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

  // LocalTime

  implicit final lazy val chooseLocalTime: Choose[LocalTime] =
    new Choose[LocalTime] {
      override def choose(min: LocalTime, max: LocalTime): Gen[LocalTime] =
        Gen.choose(min.toNanoOfDay, max.toNanoOfDay).map(nano =>
          LocalTime.ofNanoOfDay(nano)
        )
    }

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
    * This has the following implication,
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
      override def choose(min: ZoneOffset, max: ZoneOffset): Gen[ZoneOffset] =
        min.compareTo(max) match {
          case 0 => Gen.const(min)
          case result if result > 0 => Gen.fail
          case _ =>
            // Looks flipped, but it is not.
            Gen.choose(max.getTotalSeconds, min.getTotalSeconds).map(value => ZoneOffset.ofTotalSeconds(value))
        }
    }

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

  // YearMonth

  implicit final lazy val chooseYearMonth: Choose[YearMonth] =
    new Choose[YearMonth] {
      override def choose(min: YearMonth, max: YearMonth): Gen[YearMonth] =
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

  // ZonedDateTime

  implicit final lazy val chooseZonedDateTime: Choose[ZonedDateTime] =
    new Choose[ZonedDateTime] {
      override def choose(min: ZonedDateTime, max: ZonedDateTime): Gen[ZonedDateTime] =
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
}
