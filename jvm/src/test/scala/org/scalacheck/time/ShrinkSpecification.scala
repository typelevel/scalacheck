package org.scalacheck.time

import java.time._
import org.scalacheck.Prop._
import org.scalacheck.Shrink._
import org.scalacheck._

object ShrinkSpecification extends Properties ("java.time Shrink"){
  property("shrink[Duration]") = forAll { (n: Duration) =>
    !shrink(n).contains(n)
  }

  property("shrink[Period]") = forAll { (n: Period) =>
    !shrink(n).contains(n)
  }
}
