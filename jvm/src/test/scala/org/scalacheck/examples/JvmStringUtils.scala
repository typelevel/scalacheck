package org.scalacheck.example


import org.scalacheck._
import Gen.{listOf, alphaStr, numChar}

object JvmStringUtils  extends Properties("Examples.JvmStringUtils"){
  import StringUtils._
  property("truncate") = Prop.forAll { (s: String, n: Int) =>
    lazy val t = StringUtils.truncate(s, n)
    if(n < 0)
      Prop.throws(classOf[StringIndexOutOfBoundsException]) { t }
    else {
      (s.length <= n && t == s) ||
        (s.length > n && t == s.take(n) + "...")
    }
  }
}

