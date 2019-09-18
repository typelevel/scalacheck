package org.scalacheck.examples

import org.scalacheck.Prop.{forAll, propBoolean}

object MathSpec extends org.scalacheck.Properties("Math") {
  property("sqrt") = forAll { n: Int =>
    (n >= 0) ==> {
      val m = math.sqrt(n.toDouble)
      math.round(m*m) == n
    }   
  }
}
