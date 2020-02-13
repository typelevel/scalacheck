/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2019 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import scala.collection.generic.Sorted
import scala.collection.immutable.Stream
import scala.collection.TraversableOnce

private[scalacheck] object ScalaVersionSpecific {
  def toLazyList[T](i: TraversableOnce[T]) = i.toStream

  type LazyList[+A] = Stream[A]
  val LazyList      = Stream

  implicit class StreamExt[+A](val s: Stream[A]) extends AnyVal {
    def lazyAppendedAll[B >: A](rest: => TraversableOnce[B]): Stream[B] = s.append(rest)
  }

  implicit class SortedExt[K, T <: Sorted[K, T]](val s: Sorted[K, T]) extends AnyVal {
    def rangeFrom(from: K): T = s.from(from)
    def rangeTo(to: K): T = s.to(to)
    def rangeUntil(until: K): T = s.until(until)
  }
}

private[scalacheck] trait GenVersionSpecific
private[scalacheck] trait CogenVersionSpecific

// Used in tests
private[scalacheck] trait GenSpecificationVersionSpecific {
  def infiniteLazyList[T](g: => Gen[T]): Gen[Stream[T]] = Gen.infiniteStream(g)
}

private[scalacheck] trait ShrinkVersionSpecific
