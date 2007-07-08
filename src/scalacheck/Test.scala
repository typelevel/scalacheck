package scalacheck

/** Test parameters */
case class TestPrms(minSuccessfulTests: Int, maxDiscardedTests: Int,
  maxSize: Int)

/** Test statistics */
case class TestStats(result: TestResult, succeeded: Int, discarded: Int)

abstract sealed class TestResult {
  def passed = this match {
    case TestPassed() => true
    case _ => false
  }
}

/** The property test passed
 */
case class TestPassed extends TestResult

/** The property was proved wrong with the given concrete arguments.
 */
case class TestFailed(args: List[String]) extends TestResult

/** The property test was exhausted, it wasn't possible to generate enough
 *  concrete arguments satisfying the preconditions to get enough passing
 *  property evaluations.
 */
case class TestExhausted extends TestResult

/** An exception was raised when trying to evaluate the property with the
 *  given concrete arguments.
 */
case class TestPropException(e: Throwable,args: List[String]) extends TestResult

/** An exception was raised when trying to generate concrete arguments
 *  for evaluating the property.
 */
case class TestGenException(e: Throwable) extends TestResult

object Test {

  import Prop._

  // Testing functions

  val defaultTestPrms = TestPrms(100,500,100)

  type TestInspector = (Option[PropRes],Int,Int) => Unit

  /** Tests a property with the given testing parameters, and returns
   *  the test results.
   */
  def check(prms: TestPrms, p: Prop): TestStats = check(prms,p, (r,s,d) => ())

  /** Tests a property with the given testing parameters, and returns
   *  the test results. <code>f</code> is a function which is called each
   *  time the property is evaluted.
   */
  def check(prms: TestPrms, p: Prop, f: TestInspector): TestStats =
  {
    abstract sealed class Either[+T,+U]
    case class Left[+T,+U](l: T) extends Either[T,U]
    case class Right[+T,+U](r: U) extends Either[T,U]

    var nd = 0
    var ns = 0
    var tr: TestResult = null

    while(tr == null)
    {
      val size = (ns * prms.maxSize) / prms.minSuccessfulTests + nd / 10
      val genprms = GenPrms(size, StdRand)
      (try { Right(p(genprms)) } catch { case e => Left(e) }) match {
        case Left(e)   => tr = TestGenException(e)
        case Right(pr) =>
          pr match {
            case None =>
              nd = nd + 1
              if(nd >= prms.maxDiscardedTests) tr = TestExhausted
            case Some(PropTrue(_)) =>
              ns = ns + 1
              if(ns >= prms.minSuccessfulTests) tr = TestPassed
            case Some(PropFalse(as)) => tr = TestFailed(as)
            case Some(PropException(e,as)) => tr = TestPropException(e,as)
          }
          f(pr,ns,nd)
      }
    }

    TestStats(tr, ns, nd)
  }

  /** Tests a property and prints results to the console
   */
  def check(p: Prop): TestStats =
  {
    def printTmp(res: Option[PropRes], succeeded: Int, discarded: Int) = {
      if(discarded > 0)
        Console.printf("\rPassed {0} tests; {1} discarded",succeeded,discarded)
      else Console.printf("\rPassed {0} tests",succeeded)
      Console.flush
    }

    val tr = check(defaultTestPrms,p,printTmp)

    tr.result match {
      case TestGenException(e) =>
        Console.printf("\r*** Exception raised when generating arguments:\n{0}\n", e)
      case TestPropException(e,args) =>
        Console.printf("\r*** Exception raised when evaluating property\n")
        Console.printf("The arguments that caused the exception was:\n{0}\n\n", args)
        Console.printf("The raised exception was:\n{0}\n", e)
      case TestFailed(args) =>
        Console.printf("\r*** Failed, after {0} successful tests:      \n", tr.succeeded)
        Console.printf("The arguments that caused the failure was:\n{0}\n\n", args)
      case TestExhausted() =>
        Console.printf(
          "\r*** Gave up, after only {1} passed tests. {0} tests were discarded.\n",
          tr.discarded, tr.succeeded)
      case TestPassed() =>
        Console.printf("\r+++ OK, passed {0} tests.                    \n", tr.succeeded)
    }

    tr
  }

}
