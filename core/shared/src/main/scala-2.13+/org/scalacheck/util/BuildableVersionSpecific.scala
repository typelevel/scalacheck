/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package org.scalacheck.util

import java.util.{ArrayList, HashMap}
import scala.collection.mutable.Builder
import scala.collection.{Map as _, *}

private[util] trait BuildableVersionSpecific {

  implicit def buildableFactory[T, C](implicit f: Factory[T, C]): Buildable[T, C] =
    new Buildable[T, C] {
      def builder = f.newBuilder
    }
}

private[util] class ArrayListBuilder[T] extends Builder[T, ArrayList[T]] {
  private val al = new ArrayList[T]
  def addOne(x: T): this.type = {
    al.add(x)
    this
  }
  def clear(): Unit = al.clear()
  def result(): ArrayList[T] = al
}

private[util] class HashMapBuilder[K, V] extends Builder[(K, V), HashMap[K, V]] {
  private val hm = new HashMap[K, V]
  def addOne(x: (K, V)): this.type = {
    val (k, v) = x
    hm.put(k, v)
    this
  }
  def clear(): Unit = hm.clear()
  def result(): HashMap[K, V] = hm
}

/** Factory instances implementing Serializable, so that the objects capturing those can be serializable too.
 */
// Named `...CanBuildFroms` for 2.12 source compatibility (`import SerializableCanBuildFroms._`)
// Can be renamed to `SerializableFactories` in a major release.
object SerializableCanBuildFroms {
  implicit def listFactory[T]: Factory[T, List[T]] =
    new Factory[T, List[T]] with Serializable {
      def fromSpecific(source: IterableOnce[T]): List[T] = List.from(source)
      def newBuilder: Builder[T, List[T]] = List.newBuilder[T]
    }

  implicit def bitsetFactory[T]: Factory[Int, BitSet] =
    new Factory[Int, BitSet] with Serializable {
      def fromSpecific(source: IterableOnce[Int]) = BitSet.fromSpecific(source)
      def newBuilder: Builder[Int, BitSet] = BitSet.newBuilder
    }

  implicit def mapFactory[T, U]: Factory[(T, U), Map[T, U]] =
    new Factory[(T, U), Map[T, U]] with Serializable {
      def fromSpecific(source: IterableOnce[(T, U)]) = Map.from(source)
      def newBuilder: Builder[(T, U), Map[T, U]] = Map.newBuilder[T, U]
    }
}
