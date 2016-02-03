/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2016 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck.util

import language.higherKinds

import collection._
import generic.CanBuildFrom

trait Buildable[T,C] {
  def builder: mutable.Builder[T,C]
  def fromIterable(it: Traversable[T]): C = {
    val b = builder
    b ++= it
    b.result()
  }
}

object Buildable {

  implicit def buildableCanBuildFrom[T,F,C](implicit c: CanBuildFrom[F,T,C]) =
    new Buildable[T,C] {
      def builder = c.apply
    }

  import java.util.ArrayList
  implicit def buildableArrayList[T] = new Buildable[T,ArrayList[T]] {
    def builder = new mutable.Builder[T,ArrayList[T]] {
      val al = new ArrayList[T]
      def +=(x: T) = {
        al.add(x)
        this
      }
      def clear() = al.clear()
      def result() = al
    }
  }

}
/*
object Buildable2 {

  implicit def buildableMutableMap[T,U] = new Buildable2[T,U,mutable.Map] {
    def builder = mutable.Map.newBuilder
  }

  implicit def buildableImmutableMap[T,U] = new Buildable2[T,U,immutable.Map] {
    def builder = immutable.Map.newBuilder
  }

  implicit def buildableMap[T,U] = new Buildable2[T,U,Map] {
    def builder = Map.newBuilder
  }

  implicit def buildableImmutableSortedMap[T: Ordering, U] = new Buildable2[T,U,immutable.SortedMap] {
    def builder = immutable.SortedMap.newBuilder
  }

  implicit def buildableSortedMap[T: Ordering, U] = new Buildable2[T,U,SortedMap] {
    def builder = SortedMap.newBuilder
  }

}
*/
