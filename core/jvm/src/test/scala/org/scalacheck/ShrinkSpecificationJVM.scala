/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package org.scalacheck

import Prop.{forAll, propBoolean}
import Shrink.shrink
import ShrinkSpecification.shrinkClosure

/** @todo
  *   should work not only JVM but also Scala.js
  */
object ShrinkSpecificationJVM extends Properties("Shrink JVM") {

  property("list") = forAll { (l: List[Int]) =>
    !shrink(l).contains(l)
  }

  property("non-empty list") = forAll { (l: List[Int]) =>
    (!l.isEmpty && l != List(0)) ==> {
      val ls = shrinkClosure(l)
      (ls.contains(Nil) && ls.contains(List(0))).labelImpl2(ls.toList.toString)
    }
  }

  property("xmap vector from list") = forAll { (v: Vector[Int]) =>
    (!v.isEmpty && v != Vector(0)) ==> {
      val vs = shrinkClosure(v)
      (vs.contains(Vector.empty) && vs.contains(Vector(0))).labelImpl2(Vector(vs: _*).toString)
    }
  }

}
