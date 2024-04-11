/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package scalacheck.tests

import scala.collection.immutable.Stream

import org.scalacheck.Gen

private[tests] object ScalaVersionSpecific {
  type LazyList[+A] = Stream[A]
  val LazyList = Stream
}

private[tests] trait GenSpecificationVersionSpecific {
  def infiniteLazyList[T](g: => Gen[T]): Gen[Stream[T]] = Gen.infiniteStream(g)
}
