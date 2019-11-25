package org.scalacheck

import Prop.{forAll, propBoolean}
import Shrink.shrink
import ShrinkSpecification.shrinkClosure

/**
  * @todo should work not only JVM but also Scala.js
  */
object ShrinkSpecificationJVM extends Properties("Shrink JVM") {

  property("list") = forAll { (l: List[Int]) =>
    !shrink(l).contains(l)
  }

  property("non-empty list") = forAll { (l: List[Int]) =>
    (!l.isEmpty && l != List(0)) ==> {
      val ls = shrinkClosure(l)
      ls.toList.toString |: (ls.contains(Nil) && ls.contains(List(0)))
    }
  }

  property("xmap vector from list") = forAll { (v: Vector[Int]) =>
    (!v.isEmpty && v != Vector(0)) ==> {
      val vs = shrinkClosure(v)
      Vector(vs: _*).toString |: (vs.contains(Vector.empty) && vs.contains(Vector(0)))
    }
  }

}
