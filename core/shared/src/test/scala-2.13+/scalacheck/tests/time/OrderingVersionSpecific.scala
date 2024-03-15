/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package scalacheck.tests.time

/** This is unused on Scala >= 2.13.
  *
  * On Scala <= 2.12 it is used to help the compiler figure out some `Ordering` instances needed for testing.
  */
trait OrderingVersionSpecific {}
