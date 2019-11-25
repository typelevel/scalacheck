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

import org.scalacheck.{Gen, Prop, Properties}

object PrettySpecification extends Properties("Pretty") {

  property("prety(null)") = Pretty.pretty(null) == "null"

  property("pretty(null: Any)") = Pretty.pretty(null: Any) == "null"

  property("break") = {
    Prop.forAll { (s: String) =>
      Prop.forAllNoShrink(Gen.oneOf("", "  ")) { lead =>
        val length = 75
        val result = Pretty.break(s, lead, length)
        result.length >= s.length
      }
    }
  }

  property("break is stacksafe") = {
    val big = "x" * 10000
    val res = Pretty.break(big, "", 1)
    Prop.passed
  }

  property("break ensures line length") =
    Prop.forAll { (input: String, lead: String, x: Int) =>
      val length = lead.length + (x & 0xff) + 1
      val res = Pretty.break(input, lead, length)
      val lines = res.split("\n")
      lines.forall(s => s.length <= length)
    }

  property("break is reversable") =
    Prop.forAll { (input: String, lead: String, x: Int) =>
      val length = lead.length + (x & 0xff) + 1
      val res = Pretty.break(input, lead, length)

      if (res.length < input.length) {
        Prop(false)
      } else {
        Prop((0 until res.length by (length + 1)).map {
          case 0 => res.substring(0, length min res.length)
          case i => res.substring(i + lead.length, (i + length) min res.length)
        }.mkString == input)
      }
    }
}
