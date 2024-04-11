/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package scalacheck.tests
package util

import org.scalacheck.{Gen, Prop, Properties}
import org.scalacheck.Prop._
import org.scalacheck.util.Pretty

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
    val size: Int = 10000
    val big = "x" * size
    val res = Pretty.break(big, "", 1)
    res.linesIterator.size ?= size
  }

  property("break ensures line length") =
    Prop.forAll { (input: String, lead: String, x: Int) =>
      val length = lead.length + (x & 0xff) + 1
      val res = Pretty.break(input, lead, length)
      val lines = res.split("\n")
      lines.forall(s => s.length <= length)
    }

  property("break is reversible") =
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
