package org.scalacheck

import java.util.ArrayList

import scala.collection.{BitSet, Factory}
import scala.collection.mutable.Builder
import rng.Seed

private[scalacheck] object ScalaVersionSpecific {
  def toLazyList[T](i: IterableOnce[T]) = LazyList.from(i)

  def listFactory[T]: Factory[T, List[T]] =
    new Factory[T, List[T]] with Serializable {
      def fromSpecific(source: IterableOnce[T]): List[T] = List.from(source)
      def newBuilder: Builder[T, List[T]] = List.newBuilder[T]
    }

  def bitsetFactory[T]: Factory[Int, BitSet] =
    new Factory[Int, BitSet] with Serializable {
      def fromSpecific(source: IterableOnce[Int]) = BitSet.fromSpecific(source)
      def newBuilder: Builder[Int, BitSet] = BitSet.newBuilder
    }

  def mapFactory[T, U]: Factory[(T, U), Map[T, U]] =
    new Factory[(T, U), Map[T, U]] with Serializable {
      def fromSpecific(source: IterableOnce[(T, U)]) = Map.from(source)
      def newBuilder: Builder[(T, U), Map[T, U]] = Map.newBuilder[T, U]
    }
}

private[scalacheck] trait GenVersionSpecific {

  /** Generates an infinite lazy list. */
  def infiniteLazyList[T](g: => Gen[T]): Gen[LazyList[T]] = {
    def unfold[A, S](z: S)(f: S => Option[(A, S)]): LazyList[A] = f(z) match {
      case Some((h, s)) => h #:: unfold(s)(f)
      case None => LazyList.empty
    }
    Gen.gen { (p, seed0) =>
      new Gen.R[LazyList[T]] {
        val result: Option[LazyList[T]] = Some(unfold(seed0)(s => Some(g.pureApply(p, s) -> s.next)))
        val seed: Seed = seed0.next
      }
    }
  }
}

private[scalacheck] trait GenSpecificationVersionSpecific

private[scalacheck] trait CogenVersionSpecific {
  implicit def cogenLazyList[A: Cogen]: Cogen[LazyList[A]] =
    Cogen.it(_.iterator)
}

private[scalacheck] class ArrayListBuilder[T] extends Builder[T, ArrayList[T]] {
  private val al = new ArrayList[T]
  def addOne(x: T): this.type = {
    al.add(x)
    this
  }
  def clear(): Unit = al.clear()
  def result(): ArrayList[T] = al
}
