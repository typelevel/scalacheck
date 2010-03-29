/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2009 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*-------------------------------------------------------------------------*/

package org.scalacheck.util

import scala.collection._

trait Buildable[C[_]] {
  def builder[T]: mutable.Builder[T,C[T]]
  def fromIterable[T](it: Traversable[T]): C[T] = {
    val b = builder[T]
    b ++= it
    b.result()
  }
}

object Buildable {

  implicit object buildableList extends Buildable[List] {
    def builder[T] = new mutable.ListBuffer[T]
  }

  implicit object buildableStream extends Buildable[Stream] {
    def builder[T] = (new mutable.ListBuffer[T]).mapResult(_.toStream)
  }

  /*
  implicit object buildableArray extends Buildable[Array] {
    def builder[T] = new mutable.ArrayBuilder[T] {}
  }
  */

  implicit object buildableMutableSet extends Buildable[mutable.Set] {
    def builder[T] = new mutable.SetBuilder(mutable.Set.empty[T])
  }

  implicit object buildableImmutableSet extends Buildable[immutable.Set] {
    def builder[T] = new mutable.SetBuilder(immutable.Set.empty[T])
  }

  implicit object buildableSet extends Buildable[Set] {
    def builder[T] = new mutable.SetBuilder(Set.empty[T])
  }

  import java.util.ArrayList
  implicit object buildableArrayList extends Buildable[ArrayList] {
    def builder[T] = new mutable.Builder[T,ArrayList[T]] {
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
