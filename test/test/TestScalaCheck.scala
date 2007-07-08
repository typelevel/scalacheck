package test

import scalacheck._
import scalacheck.Gen._
import scalacheck.Prop._
import scalacheck.Test._

object TestScalaCheck extends Testable {

  val passing = Prop.property ((l1: List[Int], l2: List[Int]) =>
    (l2.reverse ::: l1.reverse) == (l1 ::: l2).reverse
  )

  val failing = Prop.property( (n: Int) =>
    scala.Math.sqrt(n * n) == n
  )

  val exhausted = Prop.property( (n: Int) =>
    (n > 0 && n < 0) ==> (n == n)
  )

  val propException = Prop.property( (l1: List[String]) =>
    l1.head == "foo"
  )

  val undefinedInt = for{
    n <- arbitrary[Int]
    m <- arbitrary[Int]
  } yield n/m

  val genException = forAll(undefinedInt)((n: Int) => (n == n)) 

  property("propPassing", Test.check(defaultTestPrms, passing).result match {
    case TestPassed() => true
    case _ => false
  })

  property("propFailing", Test.check(defaultTestPrms, failing).result match {
    case TestFailed(_) => true
    case _ => false
  })

  property("propExhausted", Test.check(defaultTestPrms, exhausted).result match {
    case TestExhausted() => true
    case _ => false
  })

  property("propPropException", Test.check(defaultTestPrms, propException).result match {
    case TestPropException(_,_) => true
    case _ => false
  })

  property("propGenException", Test.check(defaultTestPrms, genException).result match {
    case TestGenException(_) => true
    case _ => false
  })

  def main(args: Array[String]) = {
    val failures = check() filter { case (_,stats) => !stats.result.passed }
    java.lang.System.exit(if(failures.isEmpty) 0 else -1)
  }
}
