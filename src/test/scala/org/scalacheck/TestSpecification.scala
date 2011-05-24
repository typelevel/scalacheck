/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2011 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*-------------------------------------------------------------------------*/

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
