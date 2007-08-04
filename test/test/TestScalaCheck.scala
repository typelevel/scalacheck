package test

object Props extends scalacheck.Testable {

  import scalacheck.Gen._
  import scalacheck.Prop._
  import scalacheck.Test._
  import scalacheck.Arbitrary._

  val passing = property(1 + 1 == 2)

  val failing = property( (n: Int) => false )

  val exhausted = property( (n: Int) =>
    (n > 0 && n < 0) ==> (n == n)
  )

  val shrinked = property( (n: Int) => false )

  val propException = property { throw new java.lang.Exception; true }

  val undefinedInt = for{
    n <- arbitrary[Int]
  } yield n/0

  val genException = forAll(undefinedInt)((n: Int) => true) 

  specify("propPassing", check(defaultParams, passing).result match {
    case _:Passed => true
    case _ => false
  })

  specify("propFailing", check(defaultParams, failing).result match {
    case _:Failed => true
    case _ => false
  })

  specify("propExhausted", check(defaultParams, exhausted).result match {
    case _:Exhausted => true
    case _ => false
  })

  specify("propPropException", check(defaultParams, propException).result match {
    case _:PropException => true
    case _ => false
  })

  specify("propGenException", check(defaultParams, genException).result match {
    case _:GenException => true
    case _ => false
  })

  specify("propShrinked", check(defaultParams, shrinked).result match {
    case Failed((a:Int,_)::Nil) => a == 0
    case _ => false
  })

}

object TestScalaCheck extends Application {

  Props.checkProperties()
  scalacheck.Gen.checkProperties()
  scalacheck.Prop.checkProperties()

}
