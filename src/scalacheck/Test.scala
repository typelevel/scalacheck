package scalacheck

object Test {

  // Types

  /** Test parameters */
  case class Params(minSuccessfulTests: Int, maxDiscardedTests: Int,
    maxSize: Int, rand: RandomGenerator)

  /** Test statistics */
  case class Stats(result: Result, succeeded: Int, discarded: Int)

  abstract sealed class Result {
    def passed = this match {
      case Passed() => true
      case _ => false
    }
  }

  /** The property test passed */
  case class Passed extends Result

  /** The property was proved wrong with the given concrete arguments.  */
  case class Failed(args: List[(Any,Int)]) extends Result

  /** The property test was exhausted, it wasn't possible to generate enough
  *  concrete arguments satisfying the preconditions to get enough passing
  *  property evaluations.
  */
  case class Exhausted extends Result

  /** An exception was raised when trying to evaluate the property with the
  *  given concrete arguments.
  */
  case class PropException(args: List[(Any,Int)], e: Throwable) 
    extends Result

  /** An exception was raised when trying to generate concrete arguments
  *  for evaluating the property.
  */
  case class GenException(e: Throwable) extends Result

  /** Property evaluation callback. */
  type TestInspector = (Option[Prop.Result],Int,Int) => Unit



  // Testing functions

  val defaultParams = Params(100,500,100,StdRand)

  /** Tests a property with the given testing parameters, and returns
   *  the test results.
   */
  def check(prms: Params, p: Prop): Stats = check(prms,p, (r,s,d) => ())

  /** Tests a property with the given testing parameters, and returns
   *  the test results. <code>f</code> is a function which is called each
   *  time the property is evaluted.
   */
  def check(prms: Params, p: Prop, f: TestInspector): Stats =
  {
    abstract sealed class Either[+T,+U]
    case class Left[+T,+U](l: T) extends Either[T,U]
    case class Right[+T,+U](r: U) extends Either[T,U]

    var discarded = 0
    var successful = 0
    var testRes: Result = null

    while(testRes == null)
    {
      val size = (successful*prms.maxSize) / prms.minSuccessfulTests + 
                 discarded / 10
      val genprms = Gen.Params(size, prms.rand)
      (try { Right(p(genprms)) } catch { case e => Left(e) }) match {
        case Left(e) => testRes = GenException(e)
        case Right(propRes) =>
          propRes match {
            case None =>
              discarded += 1
              if(discarded >= prms.maxDiscardedTests) testRes = Exhausted
            case Some(Prop.True(_)) =>
              successful += 1
              if(successful >= prms.minSuccessfulTests) testRes = Passed
            case Some(Prop.False(as)) => testRes = Failed(as)
            case Some(Prop.Exception(as,e)) => testRes = PropException(as,e)
          }
          f(propRes,successful,discarded)
      }
    }

    Stats(testRes, successful, discarded)
  }

  /** Tests a property and prints results to the console
   */
  def check(p: Prop): Stats =
  {
    def printTmp(res: Option[Prop.Result], succeeded: Int, discarded: Int) = {
      if(discarded > 0)
        Console.printf("\rPassed {0} tests; {1} discarded",succeeded,discarded)
      else Console.printf("\rPassed {0} tests",succeeded)
      Console.flush
    }

    val tr = check(defaultParams,p,printTmp)

    tr.result match {
      case GenException(e) =>
        Console.printf("\r*** Exception raised when generating arguments:\n{0}\n", e)
      case PropException(args,e) =>
        Console.printf("\r*** Exception raised when evaluating property\n")
        Console.printf("The arguments that caused the failure was:\n{0}\n\n", args)
        Console.printf("The raised exception was:\n{0}\n", e)
      case Failed(args) =>
        Console.printf("\r*** Failed, after {0} successful tests:      \n", tr.succeeded)
        Console.printf("The arguments that caused the failure was:\n{0}\n\n", args)
      case Exhausted() =>
        Console.printf(
          "\r*** Gave up, after only {1} passed tests. {0} tests were discarded.\n",
          tr.discarded, tr.succeeded)
      case Passed() =>
        Console.printf("\r+++ OK, passed {0} tests.                    \n", tr.succeeded)
    }

    tr
  }

}
