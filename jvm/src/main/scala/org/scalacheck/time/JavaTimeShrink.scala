package org.scalacheck.time

import org.scalacheck._
import java.time._

/** [[Shrink]] instances for `java.time` types. */
private[scalacheck] trait JavaTimeShrink {

  // Duration

  implicit final lazy val shrinkJavaDuration: Shrink[Duration] =
    Shrink[Duration]{value =>
      val q: Duration = value.dividedBy(2)
      if (q == Duration.ZERO) {
        Stream(Duration.ZERO)
      } else {
        q #:: q.negated #:: shrinkJavaDuration.shrink(q)
      }
    }

  // Period

  implicit final lazy val shrinkPeriod: Shrink[Period] =
    Shrink.xmap[(Int, Int, Int), Period](
      {
        case (y, m, d) => Period.of(y, m, d)
      },
      value => (value.getYears, value.getMonths, value.getDays)
    )
}
