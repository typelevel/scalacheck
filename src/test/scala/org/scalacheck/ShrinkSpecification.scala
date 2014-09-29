/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2014 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import Prop.{forAll, BooleanOperators}
import Shrink.shrink

object ShrinkSpecification extends Properties("Shrink") {

  private def shrinkClosure[T : Shrink](x: T): Stream[T] = {
    val xs = shrink[T](x)
    if(xs.isEmpty) xs
    else xs.append(xs.take(1).map(shrinkClosure[T]).flatten)
  }

  property("int") = forAll { n: Int =>
    !shrink(n).contains(n)
  }

  property("list") = forAll { l: List[Int] =>
    !shrink(l).contains(l)
  }

  property("non-zero int") = forAll { n: Int =>
    (n != 0) ==> shrinkClosure(n).contains(0)
  }

  property("non-empty list") = forAll { l: List[Int] =>
    (!l.isEmpty && l != List(0)) ==> {
      val ls = shrinkClosure(l)
      ls.toList.toString |: (ls.contains(Nil) && ls.contains(List(0)))
    }
  }

  implicit def vectorShrink[A: Shrink] = Shrink.xmap[List[A],Vector[A]](Vector(_: _*), _.toList)
  property("xmap vector from list") = forAll { v: Vector[Int] ⇒
    (!v.isEmpty && v != Vector(0)) ==> {
      val vs = shrinkClosure(v)
      Vector(vs: _*).toString |: (vs.contains(Vector.empty) && vs.contains(Vector(0)))
    }
  }
}
