package scalacheck

import Util._

object Test {

  // Types

  /** Test parameters */
  case class Params(minSuccessfulTests: Int, maxDiscardedTests: Int,
    minSize: Int, maxSize: Int, rand: RandomGenerator)

  /** Test statistics */
  case class Stats(result: Result, succeeded: Int, discarded: Int) {
    def pretty = result match {
      case Passed() =>
        "OK, passed " + succeeded + " tests."
      case Failed(args) =>
        "Falsified after " + succeeded + " passed tests:\n" + prettyArgs(args)
      case Exhausted() =>
        "Gave up after only " + succeeded + " passed tests. " +
        discarded + " tests were discarded."
      case PropException(args,e) =>
        "Exception \"" + e + "\" raised on property evaluation:\n" +
        prettyArgs(args)
      case GenException(e) =>
        "Exception \"" + e + "\" raised on argument generation."
    }

    def prettyArgs(args: List[(Any,Int)]) = {
      val strs = for((arg,shrinks) <- args) yield
        "> " + arg + (if(shrinks > 0) " (" + shrinks + " shrinks)" else "")
      strs.mkString("\n")
    }
  }

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
   *  property evaluations. */
  case class Exhausted extends Result

  /** An exception was raised when trying to evaluate the property with the
   *  given concrete arguments. */
  case class PropException(args: List[(Any,Int)], e: Throwable) extends Result

  /** An exception was raised when trying to generate concrete arguments
   *  for evaluating the property. */
  case class GenException(e: Throwable) extends Result

  /** Property evaluation callback. */
  type PropEvalCallback = (Option[Prop.Result],Int,Int) => Unit



  // Testing functions

  val defaultParams = Params(100,500,0,100,StdRand)

  /** Tests a property with the given testing parameters, and returns
   *  the test results. */
  def check(prms: Params, p: Prop): Stats = check(prms,p, (r,s,d) => ())

  /** Tests a property with the given testing parameters, and returns
   *  the test results. <code>propCallback</code> is a function which is
   *  called each time the property is evaluted. */
  def check(prms: Params, p: Prop, propCallback: PropEvalCallback): Stats =
  {
    def stats(s: Int, d: Int): Stats = {
      def genprms = Gen.Params(size, prms.rand)
      def size = scala.Math.min(prms.maxSize, prms.minSize + d/10 +
        (s * (prms.maxSize-prms.minSize)) / prms.minSuccessfulTests)
 
      secure(p(genprms)) match {
        case Left(propRes) => 
          propCallback(propRes,s,d)
          propRes match {
            case None =>
              if(d+1 >= prms.maxDiscardedTests) Stats(Exhausted,s,d+1)
              else stats(s,d+1)
            case Some(_:Prop.True) =>
              if(s+1 >= prms.minSuccessfulTests) Stats(Passed,s+1,d)
              else stats(s+1,d)
            case Some(Prop.False(as)) => Stats(Failed(as),s,d)
            case Some(Prop.Exception(as,e)) => Stats(PropException(as,e),s,d)
          }
        case Right(e) => Stats(GenException(e),s,d)
      }
    }

    stats(0,0)
  }

  /** Tests a property and prints results to the console */
  def check(p: Prop): Stats =
  {
    def printPropEval(res: Option[Prop.Result], succeeded: Int, discarded: Int) = {
      if(discarded == 0) printf("\rPassed {0} tests",succeeded)
      else printf("\rPassed {0} tests; {1} discarded",succeeded,discarded)
      Console.flush
    }

    val testStats = check(defaultParams,p,printPropEval)
    val s = testStats.pretty
    printf("\r{2} {0}{1}\n", s, List.make(70 - s.length, " ").mkString(""), 
      if(testStats.result.passed) "+" else "!")
    testStats
  }

}
