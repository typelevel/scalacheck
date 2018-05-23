/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2018 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck.util

import java.util.ArrayList

import collection.{Map => _, _}
import generic.CanBuildFrom
import scala.collection.mutable.Builder

private[util] trait BuildableVersionSpecific {
  implicit def buildableCanBuildFrom[T,F,C](implicit c: CanBuildFrom[F,T,C]) =
    new Buildable[T,C] {
      def builder = c.apply
    }
}

private[util] class ArrayListBuilder[T] extends Builder[T, ArrayList[T]] {
  private val al = new ArrayList[T]
  def +=(x: T): this.type = {
    al.add(x)
    this
  }
  def clear(): Unit = al.clear()
  def result(): ArrayList[T] = al
}

/**
 * CanBuildFrom instances implementing Serializable, so that the objects capturing those can be
 * serializable too.
 */
object SerializableCanBuildFroms {
  implicit def listCanBuildFrom[T]: CanBuildFrom[List[T], T, List[T]] =
    new CanBuildFrom[List[T], T, List[T]] with Serializable {
      def apply(from: List[T]) = List.newBuilder[T]
      def apply() = List.newBuilder[T]
    }

  implicit def bitsetCanBuildFrom[T]: CanBuildFrom[BitSet, Int, BitSet] =
    new CanBuildFrom[BitSet, Int, BitSet] with Serializable {
      def apply(from: BitSet) = BitSet.newBuilder
      def apply() = BitSet.newBuilder
    }

  implicit def mapCanBuildFrom[T, U]: CanBuildFrom[Map[T, U], (T, U), Map[T, U]] =
    new CanBuildFrom[Map[T, U], (T, U), Map[T, U]] with Serializable {
      def apply(from: Map[T, U]) = Map.newBuilder[T, U]
      def apply() = Map.newBuilder[T, U]
    }
}
