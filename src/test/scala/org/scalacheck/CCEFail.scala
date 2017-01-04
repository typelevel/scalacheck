/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2013 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import Gen._
import Prop.forAll
import Arbitrary._

object GenPrimitives extends Properties("Arrayfail") {
  property("array forAll") = {
    val arrGen: Gen[Array[_]] = oneOf(
      arbitrary[Array[Int]],
      arbitrary[Array[Array[Int]]]
    )

    forAll(arrGen, arrGen) { (c1, c2) => (c1 eq c2) || (c1 ne c2) }
  }
}
