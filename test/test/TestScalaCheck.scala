package test

object Props extends scalacheck.Testable {

  import scalacheck.Gen._
  import scalacheck.Prop._
  import scalacheck.Test._
  import scalacheck.Arbitrary._

  addProperty("Test side-effect", () => { Console.println("SIDE_EFFECT"); false })

  Console.println("ADDED SIDE-EFFECT PROPERTY")

  addProperty("Test 4 params", (n: Int, m: Boolean, x: Int, y: Int) => m)

  addProperty("Test shrink (no shrink)", forAll(arbitrary[Int]) { n =>
    val g = n + 1
    n == g
  })

  addProperty("Test shrink (shrink)", forAllShrink(arbitrary[Int], (n: Int) => if(n > 1) List(n-1,n-2) else (if(n < 0) List(n+1) else Nil)) { n =>
    val g = n + 1
    n == g
  })

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

  addProperty("propPassing", () => check(defaultParams, passing).result match {
    case _:Passed => true
    case _ => false
  })

  addProperty("propFailing", () => check(defaultParams, failing).result match {
    case _:Failed => true
    case _ => false
  })

  addProperty("propExhausted", () => check(defaultParams, exhausted).result match {
    case _:Exhausted => true
    case _ => false
  })

  addProperty("propPropException", () => check(defaultParams, propException).result match {
    case _:PropException => true
    case _ => false
  })

  addProperty("propGenException", () => check(defaultParams, genException).result match {
    case _:GenException => true
    case _ => false
  })

}

object TestScalaCheck extends Application {

  Props.checkProperties()
  scalacheck.Gen.checkProperties()
  scalacheck.Prop.checkProperties()

}
