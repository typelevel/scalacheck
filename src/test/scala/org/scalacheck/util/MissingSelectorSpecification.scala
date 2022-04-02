package org.scalacheck
package util

import org.scalacheck.{Gen, Properties}
import org.scalacheck.Prop.forAll
import org.scalacheck.util.MissingSelector._

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

  // Red black tree invariants
  property("no red node has a red child") = {
    def redRed(selector: MissingSelector): Boolean = selector match {
      case Inner(R, _, Inner(R, _, _, _), _) | Inner(R, _, _, Inner(R, _, _, _)) => true
      case Inner(_, _, left, right)                                              => redRed(left) || redRed(right)
      case _                                                                     => false
    }
    forAll(missingSelectorGen)(selector => !redRed(selector))
  }

  property("all paths root-leaf have the same number of black nodes") = {
    def checkBlacks(selector: MissingSelector): Either[Unit, Int] = selector match {
      case Inner(color, _, left, right) =>
        val numRoot = color match {
          case B => 1
          case R => 0
        }
        for {
          numLeft  <- checkBlacks(left)
          numRight <- checkBlacks(right)
          res      <- if (numLeft == numRight) Right(numRoot + numLeft) else Left(())
        } yield res
      case _                            => Right(1)
    }
    forAll(missingSelectorGen)(selector => checkBlacks(selector).isRight)
  }
}