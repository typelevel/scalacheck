/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package org.scalacheck
package util

import org.scalacheck.{Gen, Prop, Properties}
import org.scalacheck.Prop.*

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

  // Counts at the edges (0, 1, total - 1, total) are where rounding used to mislead, so generate them often.
  private val totalAndCount: Gen[(Int, Int)] =
    for {
      total <- Gen.choose(1, 100000)
      count <- Gen.oneOf(Gen.const(0), Gen.const(1), Gen.const(total - 1), Gen.const(total), Gen.choose(0, total))
    } yield (total, count)

  property("percentage shows 0% and 100% only for exact counts") =
    Prop.forAll(totalAndCount) { case (total, count) =>
      val p = Pretty.percentage(count, total)
      if (count == 0) p ?= "0%"
      else if (count == total) p ?= "100%"
      else Prop(p == "<1%" || p == ">99%" || (1 to 99).exists(n => p == s"$n%")) :| s"percentage: $p"
    }

  property("prettyFreqMap shows counts and keeps rare classes visible") = {
    val fm = (1 to 999).foldLeft(FreqMap.empty[Set[Any]] + Set("rare"))((m, _) => m + Set("common"))
    Pretty.pretty(fm) ?= "> Collected test data: \n>99% (999) common\n<1% (1) rare"
  }

  property("prettyFreqMap skips data without a class") = {
    val fm = FreqMap.empty[Set[Any]] + Set("a") + Set(()) + Set("a") + Set(())
    Pretty.pretty(fm) ?= "> Collected test data: \n50% (2) a"
  }
}
