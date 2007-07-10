package test

object Props extends scalacheck.Testable {

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

  addProperty("propPassing", () => check(defaultParams, passing).result match {
    case Passed() => true
    case _ => false
  })

  addProperty("propFailing", () => check(defaultParams, failing).result match {
    case Failed(_) => true
    case _ => false
  })

  addProperty("propExhausted", () => check(defaultParams, exhausted).result match {
    case Exhausted() => true
    case _ => false
  })

  addProperty("propPropException", () => check(defaultParams, propException).result match {
    case PropException(_,_) => true
    case _ => false
  })

  addProperty("propGenException", () => check(defaultParams, genException).result match {
    case GenException(_) => true
    case _ => false
  })

}

object TestScalaCheck extends Application {

  Props.checkProperties()
  scalacheck.Gen.checkProperties()

}
