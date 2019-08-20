/*
rule = v1_14_1
 */
package fix

import org.scalacheck._

object TestBooleanOperators {

  def implicitUsage = {
    import Prop.{forAll, BooleanOperators}

    val propMakeList = forAll { n: Int =>
      (n >= 0 && n < 10000) ==> (List.fill(n)("").length == n)
    }
  }

  def explicitUsage1 = {
    val propMakeList = Prop.forAll { n: Int =>
      Prop.BooleanOperators(n >= 0 && n < 10000) ==> (List.fill(n)("").length == n)
    }
  }

  def explicitUsage2 = {
    import Prop._

    val propMakeList2 = Prop.forAll { n: Int =>
      BooleanOperators(n >= 0 && n < 10000) ==> (List.fill(n)("").length == n)
    }
  }
}
