/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2013 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck.util

import collection._

trait Buildable[T,C[_]] {
  def builder: mutable.Builder[T,C[T]]
  def fromIterable(it: Traversable[T]): C[T] = {
    val b = builder
    b ++= it
    b.result()
  }
  // TODO def toIterable
}

trait Buildable2[T,U,C[_,_]] {
  def builder: mutable.Builder[(T,U),C[T,U]]
  def fromIterable(it: Traversable[(T,U)]): C[T,U] = {
    val b = builder
    b ++= it
    b.result()
  }
}

object Buildable {

  implicit def buildableList[T] = new Buildable[T,List] {
    def builder = List.newBuilder
  }

  implicit def buildableStream[T] = new Buildable[T,Stream] {
    def builder = Stream.newBuilder
  }

  // TODO: Change ClassManifest to ClassTag when support for Scala 2.9.x can
  // be dropped

  implicit def buildableArray[T : reflect.ClassManifest] = new Buildable[T,Array] {
    def builder = mutable.ArrayBuilder.make[T]
  }

  implicit def buildableMutableSet[T] = new Buildable[T,mutable.Set] {
    def builder = mutable.Set.newBuilder
  }

  implicit def buildableImmutableSet[T] = new Buildable[T,Set] {
    def builder = new mutable.SetBuilder(Set.empty[T])
  }

  implicit def buildableImmutableSortedSet[T: Ordering] = new Buildable[T,immutable.SortedSet] {
    def builder = immutable.SortedSet.newBuilder
  }

  implicit def buildableSet[T] = new Buildable[T,Set] {
    def builder = Set.newBuilder
  }

  implicit def buildableSortedSet[T: Ordering] = new Buildable[T,SortedSet] {
    def builder = SortedSet.newBuilder
  }

  import java.util.ArrayList
  implicit def buildableArrayList[T] = new Buildable[T,ArrayList] {
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
