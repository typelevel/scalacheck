/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2019 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck
package util

import org.scalacheck.Properties

object PrettySpecification extends Properties("Pretty") {

  property("prety(null)") = Pretty.pretty(null) == "null"

  property("pretty(null: Any)") = Pretty.pretty(null: Any) == "null"

  property("break") = {
    Prop.forAll { s: String =>
      Prop.forAllNoShrink(Gen.oneOf("", "  ")) { lead =>
        val length = 75
        val result = Pretty.break(s, lead, length)
        result.length >= s.length
      }
    }
  }
}
