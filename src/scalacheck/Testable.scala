package scalacheck

import scalacheck.Prop._
import scala.collection.Map

trait Testable {

  private var properties = scala.collection.immutable.Map.empty[String, Prop]

  protected def property[A1,P]
    (propName: String, f: A1 => P)(implicit
     p:  P => Prop,
     g1: Arbitrary[A1] => Gen[A1]): Unit =
  {
    properties = properties.update(propName,Prop.property(f)(p,g1))
  }

  protected def property[A1,A2,P]
    (propName: String, f: (A1,A2) => P)(implicit
     p:  P => Prop,
     g1: Arbitrary[A1] => Gen[A1],
     g2: Arbitrary[A2] => Gen[A2]): Unit =
  {
    properties = properties.update(propName,Prop.property(f)(p,g1,g2))
  }

  protected def property[A1,A2,A3,P]
    (propName: String, f: (A1,A2,A3) => P)(implicit
     p:  P => Prop,
     g1: Arbitrary[A1] => Gen[A1],
     g2: Arbitrary[A2] => Gen[A2],
     g3: Arbitrary[A3] => Gen[A3]): Unit =
  {
    properties = properties.update(propName,Prop.property(f)(p,g1,g2,g3))
  }

  protected def property[A1,A2,A3,A4,P]
    (propName: String, f: (A1,A2,A3,A4) => P)(implicit
     p:  P => Prop,
     g1: Arbitrary[A1] => Gen[A1],
     g2: Arbitrary[A2] => Gen[A2],
     g3: Arbitrary[A2] => Gen[A3],
     g4: Arbitrary[A3] => Gen[A4]): Unit =
  {
    properties = properties.update(propName,Prop.property(f)(p,g1,g2,g3,g4))
  }

  protected def property(propName: String, prop: Prop): Unit =
    properties = properties.update(propName, prop)

  type TestsInspector = (String,Option[PropRes],Int,Int) => Unit

  /** Tests all properties with the given testing parameters, and returns
   *  the test results. <code>f</code> is a function which is called each
   *  time a property is evaluted.
   */
  def check(prms: TestPrms, f: TestsInspector): Map[String,TestStats] =
    properties transform { case (pName,p) => Test.check(prms,p,f(pName,_,_,_)) }

  def check(): Map[String,TestStats] =
  {
    def printTmp(pn: String, res: Option[PropRes], succ: Int, disc: Int) = {
      if(disc > 0)
        Console.printf("\r[{3}]: Passed {0} tests; {1} discarded",succ,disc,pn)
      else Console.printf("\r[{1}]: Passed {0} tests",succ,pn)
      Console.flush
    }

    val stats = check(Test.defaultTestPrms,printTmp)
    val failures = stats filter { case (_,stats) => !stats.result.passed }

    if(failures.isEmpty)
      Console.printf("\r+++ OK, all properties passed.                      \n")
    else failures foreach {
      case (pName,stats) => stats.result match {
        case TestGenException(e) =>
          Console.printf("\r*** [{1}]: Exception raised when generating arguments:\n{0}\n", e, pName)
        case TestPropException(e,args) =>
          Console.printf("\r*** [{0}]: Exception raised when evaluating property\n", pName)
          Console.printf("The arguments that caused the exception was:\n{0}\n\n", args)
          Console.printf("The raised exception was:\n{0}\n", e)
        case TestFailed(args) =>
          Console.printf("\r*** [{1}]: Failed, after {0} tests:                  \n", stats.succeeded, pName)
          Console.printf("The arguments that caused the failure was:\n{0}\n\n", args)
        case TestExhausted() =>
          Console.printf("\r*** [{2}]: Gave up, after only {1} passed tests. {0} tests were discarded.\n",
            stats.discarded, stats.succeeded, pName)
        case TestPassed() => ()
      }
    }

    stats
  }

}
