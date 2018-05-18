package org.scalacheck

import java.util.ArrayList

import scala.collection.generic.{CanBuildFrom, Sorted}
import scala.collection.immutable.Stream
import scala.collection.mutable.Builder
import scala.collection.{BitSet, TraversableOnce}

private[scalacheck] object ScalaVersionSpecific {
  def toLazyList[T](i: TraversableOnce[T]) = i.toStream

  type Factory[-A, +C] = CanBuildFrom[Nothing, A, C]

  def listFactory[T]: CanBuildFrom[List[T], T, List[T]] =
    new CanBuildFrom[List[T], T, List[T]] with Serializable {
      def apply(from: List[T]) = List.newBuilder[T]
      def apply() = List.newBuilder[T]
    }

  def bitsetFactory[T]: CanBuildFrom[BitSet, Int, BitSet] =
    new CanBuildFrom[BitSet, Int, BitSet] with Serializable {
      def apply(from: BitSet) = BitSet.newBuilder
      def apply() = BitSet.newBuilder
    }

  def mapFactory[T, U]: CanBuildFrom[Map[T, U], (T, U), Map[T, U]] =
    new CanBuildFrom[Map[T, U], (T, U), Map[T, U]] with Serializable {
      def apply(from: Map[T, U]) = Map.newBuilder[T, U]
      def apply() = Map.newBuilder[T, U]
    }

  implicit class CBFExt[-A, +C](val cbf: CanBuildFrom[Nothing, A, C]) extends AnyVal {
    def newBuilder: Builder[A, C] = cbf()
  }

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

private[scalacheck] class ArrayListBuilder[T] extends Builder[T, ArrayList[T]] {
  private val al = new ArrayList[T]
  def +=(x: T): this.type = {
    al.add(x)
    this
  }
  def clear(): Unit = al.clear()
  def result(): ArrayList[T] = al
}
