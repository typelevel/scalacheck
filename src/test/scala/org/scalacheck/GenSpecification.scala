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
import Prop.{forAll, someFailing, noneFailing, sizedProp}
import Arbitrary._
import Shrink._

object GenSpecification extends Properties("Gen") {

  property("sequence") =
    forAll(listOf(frequency((10,value(arbitrary[Int])),(1,value(fail)))))(l =>
      (someFailing(l) && (sequence[List,Int](l) == fail)) ||
      (noneFailing(l) && forAll(sequence[List,Int](l)) { _.length == l.length })
    )

  property("lzy") = forAll((g: Gen[Int]) => lzy(g) == g)

  property("wrap") = forAll((g: Gen[Int]) => wrap(g) == g)

  property("value") = forAll((x:Int, prms:Params) => value(x)(prms) == Some(x))

  property("fail") = forAll((prms: Params) => fail(prms) == None)

  property("choose-int") = forAll { (l: Int, h: Int) =>
    if(l > h) choose(l,h) == fail
    else forAll(choose(l,h)) { x => x >= l && x <= h }
  }

  property("choose-long") = forAll { (l: Long, h: Long) =>
    if(l > h || h-l < 0) choose(l,h) == fail
    else forAll(choose(l,h)) { x => x >= l && x <= h }
  }

  property("choose-double") = forAll { (l: Double, h: Double) =>
    if(l > h || h-l > Double.MaxValue) choose(l,h) == fail
    else forAll(choose(l,h)) { x => x >= l && x <= h }
  }

  property("parameterized") = forAll((g: Gen[Int]) => parameterized(p=>g) == g)

  property("sized") = forAll((g: Gen[Int]) => sized(i => g) == g)

  property("oneOf n") = forAll { l: List[Int] =>
    if(l.isEmpty) oneOf(l) == fail
    else forAll(oneOf(l))(l.contains)
  }

  property("oneOf 2") = forAll { (n1:Int, n2:Int) =>
    forAll(oneOf(n1, n2)) { n => n == n1 || n == n2 }
  }

  property("oneOf 2 gens") = forAll { (n1:Int, n2:Int) =>
    val g1 = Gen.value(n1)
    val g2 = Gen.value(n2)
    forAll(oneOf(g1, g2)) { n => n == n1 || n == n2 }
  }

  property("|") = forAll { (n1:Int, n2:Int, n3: Int) =>
    val g1 = Gen.value(n1)
    val g2 = Gen.value(n2)
    val g3 = Gen.value(n3)
    forAll(g1 | g2 | g3) { n => n == n1 || n == n2 || n == n3}
  }

  property("listOf") = sizedProp { sz =>
    forAll(listOf(arbitrary[Int])) { l => 
      l.length >= 0 && l.length <= sz
    }
  }

  property("listOf1") = sizedProp { sz =>
    forAll(listOf1(arbitrary[Int])) { l => 
      l.length > 0 && l.length <= math.max(1,sz)
    }
  }

  property("listOfN") = forAll(choose(0,100)) { n =>
    forAll(listOfN(n, arbitrary[Int])) { _.length == n }
  }

  property("someOf") = forAll { l: List[Int] =>
    forAll(someOf(l))(_.toList.forall(l.contains))
  }

  property("pick") = forAll { l: List[Int] =>
    forAll(choose(-1, 2*l.length)) { n =>
      if(n < 0 || n > l.length) pick(n,l) == fail
      else forAll(pick(n,l)) { m => m.length == n && m.forall(l.contains) }
    }
  }

  property("numChar") = forAll(numChar)(_.isDigit)

  property("alphaUpperChar") = forAll(alphaUpperChar) { c => 
    c.isLetter && c.isUpper
  }

  property("alphaLowerChar") = forAll(alphaLowerChar) { c => 
    c.isLetter && c.isLower
  }

  property("alphaChar") = forAll(alphaChar)(_.isLetter)

  property("alphaNumChar") = forAll(alphaNumChar)(_.isLetterOrDigit)

  property("identifier") = forAll(identifier) { s =>
    s.length > 0 && s(0).isLetter && s(0).isLower &&
    s.forall(_.isLetterOrDigit)
  }

  property("resultOf1") = forAll(resultOf((m: Int) => 0))(_ == 0)

  property("resultOf2") = {
    case class A(m: Int, s: String)
    forAll(resultOf(A)) { a:A => true }
  }

  property("resultOf3") = {
    case class B(n: Int, s: String, b: Boolean)
    implicit val arbB = Arbitrary(resultOf(B))
    forAll { b:B => true }
  }
}
