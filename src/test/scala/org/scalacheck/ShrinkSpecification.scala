/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2016 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import Prop.{forAll, BooleanOperators}
import Shrink.shrink

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

  implicit def vectorShrink[A: Shrink] = Shrink.xmap[List[A],Vector[A]](Vector(_: _*), _.toList)

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
}
