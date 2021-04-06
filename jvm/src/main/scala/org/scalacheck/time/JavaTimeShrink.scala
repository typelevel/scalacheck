/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

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
