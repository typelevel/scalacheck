package fix

import org.scalacheck._

object TestBooleanOperators {

  def implicitUsage = {
    import Prop.{forAll, propBoolean}

    val propMakeList = forAll { n: Int =>
      (n >= 0 && n < 10000) ==> (List.fill(n)("").length == n)
    }
  }

  def explicitUsage1 = {
    val propMakeList = Prop.forAll { n: Int =>
      Prop.propBoolean(n >= 0 && n < 10000) ==> (List.fill(n)("").length == n)
    }
  }

  def explicitUsage2 = {
    import Prop._

    val propMakeList2 = Prop.forAll { n: Int =>
      propBoolean(n >= 0 && n < 10000) ==> (List.fill(n)("").length == n)
    }
  }
}
