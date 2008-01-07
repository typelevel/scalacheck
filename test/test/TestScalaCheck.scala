package test

object Props extends org.scalacheck.Properties {

  import org.scalacheck._
  import org.scalacheck.Gen._
  import org.scalacheck.Prop._
  import org.scalacheck.Test._
  import org.scalacheck.Arbitrary._

  val passing = property(1 + 1 == 2)

  val failing = property( (n: Int) => false )

  val exhausted = property( (n: Int) =>
    (n > 0 && n < 0) ==> (n == n)
  )

  val shrinked = property( (t: (Int,Int,Int)) => false )

  val propException = property { throw new java.lang.Exception; true }

  val undefinedInt = for{
    n <- arbitrary[Int]
  } yield n/0

  val genException = forAll(undefinedInt)((n: Int) => true)

  specify("propFailing", (prms: Test.Params) =>
    check(prms, failing).result match {
      case _:Failed => true
      case _ => false
    }
  )

  specify("propPassing", (prms: Test.Params) =>
    check(prms, passing).result match {
      case _:Passed => true
      case _ => false
    }
  )

  specify("propExhausted", (prms: Test.Params) =>
    check(prms, exhausted).result match {
      case _:Exhausted => true
      case _ => false
    }
  )

  specify("propPropException", (prms: Test.Params) =>
    check(prms, propException).result match {
      case _:PropException => true
      case _ => false
    }
  )

  specify("propGenException", (prms: Test.Params) =>
    check(prms, genException).result match {
      case _:GenException => true
      case _ => false
    }
  )

  specify("propShrinked", (prms: Test.Params) =>
    check(prms, shrinked).result match {
      case Failed(Arg(_,(x:Int,y:Int,z:Int),_)::Nil) => 
        x == 0 && y == 0 && z == 0
      case _ => false
    }
  )

}

object TestScalaCheck extends Application {

  import org.scalacheck._
  import ConsoleReporter._

  val prms = Test.Params(100, 500, 0, 10000, StdRand)

  Props.checkProperties(prms, propReport, testReport)
  org.scalacheck.Gen.checkProperties(prms, propReport, testReport)
  org.scalacheck.Prop.checkProperties(prms, propReport, testReport)

}
