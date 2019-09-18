/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2019 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import Prop.{forAll, forAllNoShrink, propBoolean}
import Shrink.shrink

import scala.concurrent.duration.{Duration, FiniteDuration}

object ShrinkSpecification extends Properties("Shrink") {

  def shrinkClosure[T : Shrink](x: T): Stream[T] = {
    val xs = shrink[T](x)
    if(xs.isEmpty) xs
    else xs.append(xs.take(1).map(shrinkClosure[T]).flatten)
  }

  property("byte") = forAll { n: Byte =>
    !shrink(n).contains(n)
  }

  property("short") = forAll { n: Short =>
    !shrink(n).contains(n)
  }

  property("int") = forAll { n: Int =>
    !shrink(n).contains(n)
  }

  property("long") = forAll { n: Long =>
    !shrink(n).contains(n)
  }

  property("float") = forAll { n: Float =>
    !shrink(n).contains(n)
  }

  property("double") = forAll { n: Double =>
    !shrink(n).contains(n)
  }

  property("duration") = forAll { n: Duration =>
    !shrink(n).contains(n)
  }

  property("finite duration") = forAll { n: FiniteDuration =>
    !shrink(n).contains(n)
  }

  property("non-zero byte") = forAll { n: Byte =>
    (n != 0) ==> shrinkClosure(n).contains(0)
  }

  property("non-zero short") = forAll { n: Short =>
    (n != 0) ==> shrinkClosure(n).contains(0)
  }

  property("non-zero int") = forAll { n: Int =>
    (n != 0) ==> shrinkClosure(n).contains(0)
  }

  property("non-zero long") = forAll { n: Long =>
    (n != 0) ==> shrinkClosure(n).contains(0)
  }

  property("non-zero float") = forAll { n: Float =>
    (math.abs(n) > 1E-5f) ==> shrinkClosure(n).contains(0)
  }

  property("non-zero double") = forAll { n: Double =>
    (math.abs(n) > 1E-5d) ==> shrinkClosure(n).contains(0)
  }

  property("non-zero finite duration") = forAll { n: FiniteDuration =>
    (n != Duration.Zero) ==> shrinkClosure(n).contains(Duration.Zero)
  }

  property("non-zero duration") = forAll { n: Duration =>
    (n.isFinite && n != Duration.Zero) ==> shrinkClosure(n).contains(Duration.Zero)
  }

  implicit def vectorShrink[A: Shrink]: Shrink[Vector[A]] = Shrink.xmap[List[A],Vector[A]](Vector(_: _*), _.toList)

  property("either shrinks") = forAll { e: Either[Int, Long] =>
    !shrink(e).contains(e)
  }

  property("either left") = forAll { i: Int =>
    val e: Either[Int, Long] = Left(i)
    shrink(e).forall(_.isLeft)
  }

  property("either right") = forAll { i: Int =>
    val e: Either[Long, Int] = Right(i)
    shrink(e).forall(_.isRight)
  }

  /* Ensure that shrink[T] terminates. (#244)
   *
   * Let's say shrinking "terminates" when the stream of values
   * becomes empty. We can empirically determine the longest possible
   * sequence for a given type before termination. (Usually this
   * involves using the type's MinValue.)
   *
   * For example, shrink(Byte.MinValue).toList gives us 15 values:
   *
   *   List(-64, 64, -32, 32, -16, 16, -8, 8, -4, 4, -2, 2, -1, 1, 0)
   *
   * Similarly, shrink(Double.MinValue).size gives us 2081.
   */

  property("Shrink[Byte] terminates") =
    forAllNoShrink((n: Byte) => Shrink.shrink(n).drop(15).isEmpty)

  property("Shrink[Char] terminates") =
    forAllNoShrink((n: Char) => Shrink.shrink(n).drop(16).isEmpty)

  property("Shrink[Short] terminates") =
    forAllNoShrink((n: Short) => Shrink.shrink(n).drop(31).isEmpty)

  property("Shrink[Int] terminates") =
    forAllNoShrink((n: Int) => Shrink.shrink(n).drop(63).isEmpty)

  property("Shrink[Long] terminates") =
    forAllNoShrink((n: Long) => Shrink.shrink(n).drop(127).isEmpty)

  property("Shrink[Float] terminates") =
    forAllNoShrink((n: Float) => Shrink.shrink(n).drop(2081).isEmpty)

  property("Shrink[Double] terminates") =
    forAllNoShrink((n: Double) => Shrink.shrink(n).drop(2081).isEmpty)

  property("Shrink[FiniteDuration] terminates") =
    forAllNoShrink((n: FiniteDuration) => Shrink.shrink(n).drop(2081).isEmpty)

  property("Shrink[Duration] terminates") =
    forAllNoShrink((n: Duration) => Shrink.shrink(n).drop(2081).isEmpty)

  // make sure we handle sentinel values appropriately for Float/Double.

  property("Shrink[Float] handles PositiveInfinity") =
    Prop(Shrink.shrink(Float.PositiveInfinity).isEmpty)

  property("Shrink[Float] handles NegativeInfinity") =
    Prop(Shrink.shrink(Float.NegativeInfinity).isEmpty)

  property("Shrink[Float] handles NaN") =
    Prop(Shrink.shrink(Float.NaN).isEmpty)

  property("Shrink[Double] handles PositiveInfinity") =
    Prop(Shrink.shrink(Double.PositiveInfinity).isEmpty)

  property("Shrink[Double] handles NegativeInfinity") =
    Prop(Shrink.shrink(Double.NegativeInfinity).isEmpty)

  property("Shrink[Double] handles NaN") =
    Prop(Shrink.shrink(Double.NaN).isEmpty)

  property("Shrink[Duration] handles Inf") =
    Prop(Shrink.shrink(Duration.Inf: Duration).isEmpty)

  property("Shrink[Duration] handles MinusInf") =
    Prop(Shrink.shrink(Duration.MinusInf: Duration).isEmpty)

  property("Shrink[Duration] handles Undefined") =
    Prop(Shrink.shrink(Duration.Undefined: Duration).isEmpty)
}
