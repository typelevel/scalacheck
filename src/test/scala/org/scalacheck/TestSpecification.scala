/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2011 Rickard Nilsson. All rights reserved.          **
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

object TestSpecification extends Properties("Test") {

  val proved: Prop = 1 + 1 == 2

  val passing = forAll( (n: Int) => n == n )

  val failing = forAll( (n: Int) => false )

  val exhausted = forAll( (n: Int) =>
    (n > 0 && n < 0) ==> (n == n)
  )

  val shrinked = forAll( (t: (Int,Int,Int)) => false )

  val propException = forAll { n:Int => throw new java.lang.Exception; true }

  val undefinedInt = for{
    n <- arbitrary[Int]
  } yield n/0

  val genException = forAll(undefinedInt)((n: Int) => true)

  property("workers") = forAll { prms: Test.Params =>
    var res = true
    
    val cb = new Test.TestCallback {
      override def onPropEval(n: String, threadIdx: Int, s: Int, d: Int) = {
        res = res && threadIdx >= 0 && threadIdx <= (prms.workers-1)
      }
    }

    Test.check(prms.copy(testCallback = cb), passing).status match {
      case Passed => res
      case _ => false
    }
  }

  property("minSuccessfulTests") = forAll { (prms: Test.Params, p: Prop) =>
    val r = Test.check(prms, p)
    r.status match {
      case Passed => r.succeeded >= prms.minSuccessfulTests
      case Exhausted => r.succeeded + r.discarded >= prms.minSuccessfulTests
      case _ => true
    }
  }

  /*
   * This seems to randomly break like with these values:
   * [info] r.status = Exhausted
   * [info] prms.maxDiscardRatio = 3.3618402
   * [info] r.discarded = 13
   * [info] prms.minSuccessfulTests = 9
   * [info] r.succeeded = 8
   * [info] > ARG_0: Params(9,3.3618402,75,305,org.scalacheck.util.StdRand$@608148fe,4,org.scalacheck.Test$Params$$anon$6@47df4d31)
   * [info] > ARG_1: Prop
  property("maxDiscardRatio") = forAll { (prms: Test.Params, p: Prop) =>
    val r = Test.check(prms, p)
    //("r.status = " + r.status) |:
    r.status match {
      case Passed => {
        ("r.status = " + r.status) |:
        ("r.succeeded = " + r.succeeded) |:
        ("prms.maxDiscardRatio = " + prms.maxDiscardRatio) |:
        ("r.discarded = " + r.discarded) |:
        r.succeeded*prms.maxDiscardRatio >= r.discarded
      }
      case Exhausted => 
        ("r.status = " + r.status) |:
        ("r.succeeded = " + r.succeeded) |:
        ("prms.maxDiscardRatio = " + prms.maxDiscardRatio) |:
        ("r.discarded = " + r.discarded) |:
        ("prms.minSuccessfulTests = " + prms.minSuccessfulTests) |:
        r.succeeded + r.discarded >= prms.minSuccessfulTests &&
        r.succeeded*prms.maxDiscardRatio < r.discarded
      case _ => 
        ("r.status = " + r.status) |:
        ("r.succeeded = " + r.succeeded) |:
        ("prms.maxDiscardRatio = " + prms.maxDiscardRatio) |:
        ("r.discarded = " + r.discarded) |:
        ("prms.minSuccessfulTests = " + prms.minSuccessfulTests) |:
        r.succeeded + r.discarded < prms.minSuccessfulTests ||
        r.succeeded*prms.maxDiscardRatio >= r.discarded
    }
  }
  */

  property("size") = forAll { prms: Test.Params =>
    val p = sizedProp { sz => sz >= prms.minSize && sz <= prms.maxSize }
    Test.check(prms, p).status match {
      case Passed => true
      case _ => false
    }
  }

  property("propFailing") = forAll { prms: Test.Params =>
    Test.check(prms, failing).status match {
      case _:Failed => true
      case _ => false
    }
  }

  property("propPassing") = forAll { prms: Test.Params =>
    Test.check(prms, passing).status match {
      case Passed => true
      case _ => false
    }
  }

  property("propProved") = forAll { prms: Test.Params =>
    Test.check(prms, proved).status match {
      case _:Test.Proved => true
      case _ => false
    }
  }

  property("propExhausted") = forAll { prms: Test.Params =>
    Test.check(prms, exhausted).status match {
      case Exhausted => true
      case _ => false
    }
  }

  property("propPropException") = forAll { prms: Test.Params =>
    Test.check(prms, propException).status match {
      case _:PropException => true
      case _ => false
    }
  }

  property("propGenException") = forAll { prms: Test.Params =>
    Test.check(prms, genException).status match {
      case _:GenException => true
      case _ => false
    }
  }

  property("propShrinked") = forAll { prms: Test.Params =>
    Test.check(prms, shrinked).status match {
      case Failed(Arg(_,(x:Int,y:Int,z:Int),_,_)::Nil,_) =>
        x == 0 && y == 0 && z == 0
      case x => false
    }
  }

}
