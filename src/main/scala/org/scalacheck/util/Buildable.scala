/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2013 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck.util

import collection.mutable

//TODO trait Buildable[T,C[_] <: Seq[_]] {
trait Buildable[T,C[_]] {
  def builder: mutable.Builder[T,C[T]]
  def fromIterable(it: Traversable[T]): C[T] = {
    val b = builder
    b ++= it
    b.result()
  }
}

object Buildable {

  implicit def buildableList[T] = new Buildable[T,List] {
    def builder = new mutable.ListBuffer[T]
  }

  implicit def buildableStream[T] = new Buildable[T,Stream] {
    def builder = (new mutable.ListBuffer[T]).mapResult(_.toStream)
  }

  // TODO: Change ClassManifest to ClassTag when support for Scala 2.9.x can
  // be dropped

  implicit def buildableArray[T : reflect.ClassManifest] = new Buildable[T,Array] {
    def builder = mutable.ArrayBuilder.make[T]
  }

  implicit def buildableMutableSet[T] = new Buildable[T,mutable.Set] {
    def builder = new mutable.SetBuilder(mutable.Set.empty[T])
  }

  implicit def buildableImmutableSet[T] = new Buildable[T,Set] {
    def builder = new mutable.SetBuilder(Set.empty[T])
  }

  implicit def buildableSet[T] = new Buildable[T,Set] {
    def builder = new mutable.SetBuilder(Set.empty[T])
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
