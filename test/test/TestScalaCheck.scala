package test

object TestScalaCheck {

  import scalacheck._
  import scalacheck.Gen._
  import scalacheck.Prop._
  import scalacheck.Test._

  val passing = property ((l1: List[Int], l2: List[Int]) =>
    (l2.reverse ::: l1.reverse) == (l1 ::: l2).reverse
  )

  val failing = property( (n: Int) =>
    scala.Math.sqrt(n * n) == n
  )

  val exhausted = property( (n: Int) =>
    (n > 0 && n < 0) ==> (n == n)
  )

  val propException = property( (l1: List[String]) =>
    l1.head == "foo"
  )

  val undefinedInt = for{
    n <- arbitrary[Int]
    m <- arbitrary[Int]
  } yield n/m

  val genException = forAll(undefinedInt)((n: Int) => (n == n)) 

  def propPassing = check(defaultTestPrms, passing).result match {
    case TestPassed() => true
    case _ => false
  }

  def propFailing = check(defaultTestPrms, failing).result match {
    case TestFailed(_) => true
    case _ => false
  }

  def propExhausted = check(defaultTestPrms, exhausted).result match {
    case TestExhausted() => true
    case _ => false
  }

  def propPropException = check(defaultTestPrms, propException).result match {
    case TestPropException(_,_) => true
    case _ => false
  }

  def propGenException = check(defaultTestPrms, genException).result match {
    case TestGenException(_) => true
    case _ => false
  }

  val props = List(
    propPassing,
    propFailing,
    propExhausted,
    propPropException,
    propGenException
  )

  def main(args: Array[String]) = {
    java.lang.System.exit(props.map(check(defaultTestPrms,_)).filter(!_.result.passed).length)
  }
}
