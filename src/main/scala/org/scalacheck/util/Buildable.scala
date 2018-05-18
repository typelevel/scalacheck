/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2018 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck.util

import scala.collection.{mutable, Map => _, _}
import org.scalacheck.{ScalaVersionSpecific, ArrayListBuilder}
import ScalaVersionSpecific._

trait Buildable[T,C] extends Serializable {
  def builder: mutable.Builder[T,C]
  def fromIterable(it: Traversable[T]): C = {
    val b = builder
    b ++= it
    b.result()
  }
}

/**
  * Factory instances implementing Serializable, so that the objects capturing those can be
  * serializable too.
  */
object SerializableCanBuildFroms {
  // Names are `..CanBuildFrom` for binary compatibility. Change to `..Factory` in a major release.
  implicit def listCanBuildFrom[T]: Factory[T, List[T]] = ScalaVersionSpecific.listFactory
  implicit def bitsetCanBuildFrom[T]: Factory[Int, BitSet] = ScalaVersionSpecific.bitsetFactory
  implicit def mapCanBuildFrom[T, U]: Factory[(T, U), Map[T, U]] = ScalaVersionSpecific.mapFactory
}

object Buildable {
  // Name is `..CanBuildFrom` for binary compatibility. Change to `..Factory` in a major release.
  implicit def buildableCanBuildFrom[T,C](implicit f: Factory[T,C]) =
    new Buildable[T,C] {
      def builder = f.newBuilder
    }

  import java.util.ArrayList
  implicit def buildableArrayList[T] = new Buildable[T,ArrayList[T]] {
    def builder = new ArrayListBuilder[T]
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
