package scalacheck

import scalacheck.Prop._
import scalacheck.Test._
import scala.collection.Map
import scala.testing.SUnit.TestCase

trait Testable {

  private var properties = scala.collection.immutable.Map.empty[String, Prop]

  protected def addProperty[P]
    (propName: String, f: () => P)(implicit
     p:  P => Prop): Unit =
  {
    properties = properties.update(propName,property(f))
  }

  protected def addProperty[A1,P]
    (propName: String, f: A1 => P)(implicit
     p:  P => Prop,
     g1: Arbitrary[A1] => Gen[A1]): Unit =
  {
    properties = properties.update(propName,property(f))
  }

  protected def addProperty[A1,A2,P]
    (propName: String, f: (A1,A2) => P)(implicit
     p:  P => Prop,
     g1: Arbitrary[A1] => Gen[A1],
     g2: Arbitrary[A2] => Gen[A2]): Unit =
  {
    properties = properties.update(propName,property(f))
  }

  protected def addProperty[A1,A2,A3,P]
    (propName: String, f: (A1,A2,A3) => P)(implicit
     p:  P => Prop,
     g1: Arbitrary[A1] => Gen[A1],
     g2: Arbitrary[A2] => Gen[A2],
     g3: Arbitrary[A3] => Gen[A3]): Unit =
  {
    properties = properties.update(propName,property(f))
  }

  protected def addProperty[A1,A2,A3,A4,P]
    (propName: String, f: (A1,A2,A3,A4) => P)(implicit
     p:  P => Prop,
     g1: Arbitrary[A1] => Gen[A1],
     g2: Arbitrary[A2] => Gen[A2],
     g3: Arbitrary[A2] => Gen[A3],
     g4: Arbitrary[A3] => Gen[A4]): Unit =
  {
    properties = properties.update(propName,property(f))
  }

  protected def addProperty(propName: String, prop: Prop): Unit =
    properties = properties.update(propName, prop)

  type TestsInspector = (String,Option[PropRes],Int,Int) => Unit
  type TestStatsInspector = (String,TestStats) => Unit

  /** Tests all properties with the given testing parameters, and returns
   *  the test results. <code>f</code> is a function which is called each
   *  time a property is evaluted. <code>g</code> is a function called each
   *  time a property has been fully tested.
   */
  def checkProperties(prms: TestPrms, f: TestsInspector, g: TestStatsInspector
  ): Map[String,TestStats] = properties transform { case (pName,p) =>
    val stats = check(prms,p,f(pName,_,_,_))
    g(pName,stats)
    stats
  }

  /** Tests all properties with default testing parameters, and returns
   *  the test results. The results are also printed on the console during
   *  testing.
   */
  def checkProperties(): Map[String,TestStats] =
  {
    def printTmp(pn: String, res: Option[PropRes], succ: Int, disc: Int) = {
      if(disc > 0)
        Console.printf("\r{3}: Passed {0} tests; {1} discarded",succ,disc,pn)
      else
        Console.printf("\r{1}: Passed {0} tests",succ,pn)
      Console.flush
    }

    def printStats(pName: String, stats: TestStats) = stats.result match {
      case TestGenException(e) =>
        Console.printf("\r{1}: *** Exception raised when generating arguments:\n{0}               \n\n",
          e, pName)
      case TestPropException(e,args) =>
        Console.printf("\r{0}: *** Exception raised when evaluating property                        \n",
          pName)
        Console.printf("The arguments that caused the exception was:\n{0}\n\n", args)
        Console.printf("The raised exception was:\n{0}\n\n", e)
      case TestFailed(args) =>
        Console.printf("\r{1}: *** Failed after {0} successful tests                                \n",
          stats.succeeded, pName)
        Console.printf("The arguments that caused the failure was:\n{0}\n\n", args)
      case TestExhausted() =>
        Console.printf("\r{2}: *** Gave up, after only {1} passed tests. {0} tests were discarded.\n\n",
          stats.discarded, stats.succeeded, pName)
      case TestPassed() =>
        Console.printf("\r{0}: +++ OK, tests passed.                                              \n\n",
          pName)
    }

    checkProperties(Test.defaultTestPrms,printTmp,printStats)
  }

  private def propToTestCase(pn: String, p: Prop): TestCase = new TestCase(pn) {

    protected def runTest() = {
      val stats = check(Test.defaultTestPrms,p)
      stats.result match {
        case TestGenException(e) => fail(
          " Exception raised when generating arguments.\n" +
          "The raised exception was:\n"+e.toString+"\n")
        case TestPropException(e,args) => fail(
          " Exception raised when evaluating property.\n\n" +
          "The arguments that caused the failure was:\n"+args.toString+"\n\n" +
          "The raised exception was:\n"+e.toString+"\n")
        case TestFailed(args) => fail(
          " Property failed after " + stats.succeeded.toString + 
          " successful tests.\n" +
          "The arguments that caused the failure was:\n"+args.toString+"\n\n")
        case TestExhausted() => fail(
          " Gave up after only " + stats.succeeded.toString + " tests. " +
          stats.discarded.toString + " tests were discarded.")
        case TestPassed() => ()
      }
    }

  }

  /** Returns all properties as SUnit.TestCase instances, which can added to
   *  a SUnit.TestSuite.
   */
  def testCases: List[TestCase] = 
    (properties map {case (pn,p) => propToTestCase(pn,p)}).toList

}
