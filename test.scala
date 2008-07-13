import org.scalacheck._
import org.scalacheck.Gen._
import org.scalacheck.Prop._
import org.scalacheck.Test._
import org.scalacheck.Arbitrary._

object ScalaCheckSpecification extends Properties("ScalaCheck") {

  val proved = property(1 + 1 == 2)

  val passing = property( (n: Int) => n == n )

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

  include(Prop.specification)
  include(Gen.specification)

  specify("propFailing", (prms: Test.Params) =>
    Test.check(prms, failing).status match {
      case _:Failed => true
      case _ => false
    }
  )

  specify("propPassing", (prms: Test.Params) =>
    Test.check(prms, passing).status match {
      case Passed => true
      case _ => false
    }
  )

  specify("propProved", (prms: Test.Params) =>
    Test.check(prms, proved).status match {
      case _:Test.Proved => true
      case _ => false
    }
  )

  specify("propExhausted", (prms: Test.Params) =>
    Test.check(prms, exhausted).status match {
      case Exhausted => true
      case _ => false
    }
  )

  specify("propPropException", (prms: Test.Params) =>
    Test.check(prms, propException).status match {
      case _:PropException => true
      case _ => false
    }
  )

  specify("propGenException", (prms: Test.Params) =>
    Test.check(prms, genException).status match {
      case _:GenException => true
      case _ => false
    }
  )

  specify("propShrinked", (prms: Test.Params) =>
    Test.check(prms, shrinked).status match {
      case Failed(Arg(_,(x:Int,y:Int,z:Int),_)::Nil) => 
        x == 0 && y == 0 && z == 0
      case x => false
    }
  )

}

val verbose = args.contains("-v")
val large = args.contains("-l")
val wrkSize = if(large) 200 else 20
val workers = 
  if(args.contains("-4")) 4 
  else if(args.contains("-2")) 2 
  else 0

val prms = 
  if(large) Test.Params(1000, 5000, 0, 10000, StdRand)
  else Test.defaultParams

val propReport: (String,Int,Int) => Unit = 
  if(verbose) ConsoleReporter.propReport 
  else (n, i, j) => () 

val testReport: (String,Test.Result) => Unit = 
  if(verbose) ConsoleReporter.testReport
  else (n, s) => s match {
    case Test.Result(Test.Passed, _, _, _) => {}
    case _ => ConsoleReporter.testReport(n,s)
  }


def measure[T](t: => T): (T,Long,Long) = {
  val start = System.currentTimeMillis
  val x = t
  val stop = System.currentTimeMillis
  (x,start,stop)
}

val (res,start,stop) = measure(
  Test.checkProperties(ScalaCheckSpecification, prms, propReport, testReport, workers, wrkSize)
)

val min = (stop-start)/(60*1000)
val sec = ((stop-start)-(60*1000*min)) / 1000d

val passed = res.filter(_._2.passed).size
val failed = res.filter(!_._2.passed).size

if(verbose || failed > 0) println

if(passed > 0) printf("%s test%s PASSED\n", passed, if(passed != 1) "s" else "")
if(failed > 0) printf("%s test%s FAILED\n", failed, if(failed != 1) "s" else "")
printf("Elapsed time: %s min %s sec\n", min, sec)

exit(failed)
