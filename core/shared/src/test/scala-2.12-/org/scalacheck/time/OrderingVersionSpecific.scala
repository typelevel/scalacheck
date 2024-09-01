/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package org.scalacheck.time

import java.time.*
import java.time.chrono.*

/** On Scala <= 2.12 it is used to help the compiler figure out some `Ordering` instances needed for testing.
 */
trait OrderingVersionSpecific {

  implicit final lazy val localDateOrdering: Ordering[LocalDate] =
    Ordering.by((ld: LocalDate) => (ld: ChronoLocalDate))

  implicit final lazy val localDateTimeOrdering: Ordering[LocalDateTime] =
    Ordering.by((ldt: LocalDateTime) => (ldt: ChronoLocalDateTime[?]))

  implicit final lazy val zonedDateTimeOrdering: Ordering[ZonedDateTime] =
    Ordering.by((zdt: ZonedDateTime) => (zdt: ChronoZonedDateTime[?]))
}
