/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2015 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import Gen._
import Prop._
import Test._
import Arbitrary._
import collection.mutable.ListBuffer

object JvmTestSpecification extends Properties("Test") {

  val undefinedInt = for{
    n <- arbitrary[Int]
  } yield n/0

  val genException = forAll(undefinedInt)((n: Int) => true)

  property("minSuccessfulTests") = forAll { (prms: Test.Parameters, p: Prop) =>
    val r = Test.check(prms, p)
    r.status match {
      case Passed => r.status+", s="+r.succeeded+", d="+r.discarded |:
        r.succeeded >= prms.minSuccessfulTests
      case Exhausted => r.status+", s="+r.succeeded+", d="+r.discarded |:
        r.succeeded + r.discarded >= prms.minSuccessfulTests
      case _ => r.status+", s="+r.succeeded+", d="+r.discarded |:
        r.succeeded < prms.minSuccessfulTests
    }
  }

  property("propGenException") = forAll { prms: Test.Parameters =>
    Test.check(prms, genException).status match {
      case x:PropException => true :| x.toString
      case x => false :| x.toString
    }
  }
}
