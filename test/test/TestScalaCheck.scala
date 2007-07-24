package test

object Props extends scalacheck.Testable {

  import scalacheck.Gen._
  import scalacheck.Prop._
  import scalacheck.Test._
  import scalacheck.Arbitrary._

  specify("Test forAllShrink", forAllShrink(arbitrary[Int],shrink[Int])(n => n == (n+1)))

  specify("Test shrink 1 (shrink)", (n: Int) => n == (n+1))

  specify("Test shrink 2 (shrink)", (n: Int, m: Int) => n == m)

  val passing = property (() => 
    1 + 1 == 2
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

  specify("propPassing", () => check(defaultParams, passing).result match {
    case _:Passed => true
    case _ => false
  })

  specify("propFailing", () => check(defaultParams, failing).result match {
    case _:Failed => true
    case _ => false
  })

  specify("propExhausted", () => check(defaultParams, exhausted).result match {
    case _:Exhausted => true
    case _ => false
  })

  specify("propPropException", () => check(defaultParams, propException).result match {
    case _:PropException => true
    case _ => false
  })

  specify("propGenException", () => check(defaultParams, genException).result match {
    case _:GenException => true
    case _ => false
  })

}

object TestScalaCheck extends Application {

  Props.checkProperties()
  scalacheck.Gen.checkProperties()
  scalacheck.Prop.checkProperties()

}
