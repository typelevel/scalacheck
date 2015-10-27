/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2015 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck.util

import collection.GenMapLike
import collection.generic.IsTraversableOnce

trait Sized[A] {
  final def isNotEmpty: A => Boolean = !isEmpty(_)
  def isEmpty(a: A): Boolean
  def size(a: A): Int
}

object Sized {
  // most single arity colections have this
  implicit def IsTraversableOnceSized[C](implicit traversable: IsTraversableOnce[C]): Sized[C] = 
    new Sized[C] {
      def isEmpty(c: C) = traversable.conversion(c).isEmpty
      def size(c: C) = traversable.conversion(c).size
    }

  // Maps don't have IsTraversableOnce
  implicit def IsMapLikeSized[M <: GenMapLike[_, _, M]]: Sized[M] = 
    new Sized[M] {
      def isEmpty(m: M) = m.isEmpty
      def size(m: M) = m.size
    }
}
