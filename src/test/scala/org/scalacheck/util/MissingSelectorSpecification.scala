package org.scalacheck
package util

import org.scalacheck.{Gen, Properties}
import org.scalacheck.Prop.forAll

object MissingSelectorSpecification extends Properties("MissingSelector") {

  private val smallIntegerGen: Gen[Int] = Gen.choose(0,1000)

  private val missingSelectorGen: Gen[MissingSelector] = Gen.listOf(smallIntegerGen).map { list =>
    list.foldLeft(MissingSelector.empty){ case (selector, elem) => selector.selectAndAdd(elem)._2 }
  }

  property("selectAndAdd adds the selected element to the selector") =
    forAll(missingSelectorGen) { selector =>
      forAll(smallIntegerGen) { i =>
        val (selected, newSelector) = selector.selectAndAdd(i)
        newSelector.toList().sorted == (selected :: selector.toList()).sorted
      }
    }

  property("selectAndAdd selects the i-th missing element") =
    forAll(missingSelectorGen) { selector =>
      forAll(smallIntegerGen) { i =>
        val (selected, _) = selector.selectAndAdd(i)
        val numNotGreater = selector.toList().filter(_ <= selected).length
        i + numNotGreater == selected
      }
    }
}