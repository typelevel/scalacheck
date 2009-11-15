/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2009 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*-------------------------------------------------------------------------*/

package org.scalacheck

import Gen._
import Prop.{forAll, someFailing, noneFailing}
import Arbitrary._
import Shrink._

object GenSpecification extends Properties("Gen") {

  property("sequence") =
    forAll(listOf(frequency((10,value(arbitrary[Int])),(1,value(fail)))))(l =>
      (someFailing(l) && (sequence[List,Int](l) === fail)) ||
      (noneFailing(l) && forAll(sequence[List,Int](l)) { _.length == l.length })
    )

  property("lzy") = forAll((g: Gen[Int]) => lzy(g) === g)

  property("wrap") = forAll((g: Gen[Int]) => wrap(g) === g)

  property("value") = forAll((x:Int, prms:Params) => value(x)(prms) == Some(x))

  property("fail") = forAll((prms: Params) => fail(prms) == None)

  property("choose-int") = forAll { (l: Int, h: Int) =>
    if(l > h) choose(l,h) === fail
    else forAll(choose(l,h)) { x => x >= l && x <= h }
  }

  property("choose-long") = forAll { (l: Long, h: Long) =>
    if(l > h) choose(l,h) === fail
    else forAll(choose(l,h)) { x => x >= l && x <= h }
  }

  property("choose-double") = forAll { (l: Double, h: Double) =>
    if(l > h) choose(l,h) === fail
    else forAll(choose(l,h)) { x => x >= l && x <= h }
  }

  property("parameterized") = forAll((g: Gen[Int]) => parameterized(p=>g) === g)

  property("sized") = forAll((g: Gen[Int]) => sized(i => g) === g)

  property("oneOf") = forAll { l: List[Int] =>
    val gs = l.map(value(_))
    if(l.isEmpty) oneOf(gs: _*) === fail
    else forAll(oneOf(gs: _*))(l.contains)
  }

  property("listOf1") = forAll(listOf1(arbitrary[Int]))(_.length > 0)

  property("listOfN") = forAll(choose(0,100)) { n =>
    forAll(listOfN(n, arbitrary[Int])) { _.length == n }
  }

  property("someOf") = forAll { l: List[Int] =>
    forAll(someOf(l))(_.toList.forall(l.contains))
  }

  property("pick") = forAll { l: List[Int] =>
    forAll(choose(-1, 2*l.length)) { n =>
      if(n < 0 || n > l.length) pick(n,l) === fail
      else forAll(pick(n,l)) { m => m.length == n && m.forall(l.contains) }
    }
  }

  property("numChar") = forAll(numChar)(_.isDigit)

  property("alphaUpperChar") = forAll(alphaUpperChar) { c => 
    c.isLetter && c.isUpperCase
  }

  property("alphaLowerChar") = forAll(alphaLowerChar) { c => 
    c.isLetter && c.isLowerCase
  }

  property("alphaChar") = forAll(alphaChar)(_.isLetter)

  property("alphaNumChar") = forAll(alphaNumChar)(_.isLetterOrDigit)

  property("identifier") = forAll(identifier) { s =>
    s.length > 0 && s(0).isLetter && s(0).isLowerCase &&
    s.forall(_.isLetterOrDigit)
  }

}
