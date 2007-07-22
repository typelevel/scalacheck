package scalacheck

trait Testable {

  import scala.collection.Map
  import scala.testing.SUnit.TestCase

  private var properties = scala.collection.immutable.Map.empty[String, Prop]

  protected def addProperty[P] (
    propName: String, f: () => P)(
    implicit p: P => Prop
  ): Unit = addProperty(propName,Prop.property(f))

  protected def addProperty[A1,P] (
    propName: String, f: A1 => P)(
    implicit p: P => Prop,
    a1: Arb[A1] => Arbitrary[A1]
  ): Unit = addProperty(propName,Prop.property(f))

  protected def addProperty[A1,A2,P] (
    propName: String, f: (A1,A2) => P)(
    implicit p: P => Prop,
    a1: Arb[A1] => Arbitrary[A1],
    a2: Arb[A2] => Arbitrary[A2]
  ): Unit = addProperty(propName,Prop.property(f))

  protected def addProperty[A1,A2,A3,P] (
    propName: String, f: (A1,A2,A3) => P)(
    implicit p: P => Prop,
    a1: Arb[A1] => Arbitrary[A1],
    a2: Arb[A2] => Arbitrary[A2],
    a3: Arb[A3] => Arbitrary[A3]
  ): Unit = addProperty(propName,Prop.property(f))

  protected def addProperty[A1,A2,A3,A4,P] (
    propName: String, f: (A1,A2,A3,A4) => P)(
    implicit p: P => Prop,
    a1: Arb[A1] => Arbitrary[A1],
    a2: Arb[A2] => Arbitrary[A2],
    a3: Arb[A3] => Arbitrary[A3],
    a4: Arb[A4] => Arbitrary[A4]
  ): Unit = addProperty(propName,Prop.property(f))

  protected def addProperty[A1,A2,A3,A4,A5,P] (
    propName: String, f: (A1,A2,A3,A4,A5) => P)(
    implicit p: P => Prop,
    a1: Arb[A1] => Arbitrary[A1],
    a2: Arb[A2] => Arbitrary[A2],
    a3: Arb[A3] => Arbitrary[A3],
    a4: Arb[A5] => Arbitrary[A5],
    a5: Arb[A4] => Arbitrary[A4]
  ): Unit = addProperty(propName,Prop.property(f))

  protected def addProperty[A1,A2,A3,A4,A5,A6,P] (
    propName: String, f: (A1,A2,A3,A4,A5,A6) => P)(
    implicit p: P => Prop,
    a1: Arb[A1] => Arbitrary[A1],
    a2: Arb[A2] => Arbitrary[A2],
    a3: Arb[A3] => Arbitrary[A3],
    a4: Arb[A4] => Arbitrary[A4],
    a5: Arb[A5] => Arbitrary[A5],
    a6: Arb[A6] => Arbitrary[A6]
  ): Unit = addProperty(propName,Prop.property(f))

  protected def addProperty(propName: String, prop: Prop) =
    properties = properties.update(propName, prop)

  type TestsInspector = (String,Option[Prop.Result],Int,Int) => Unit
  type TestStatsInspector = (String,Test.Stats) => Unit

  /** Tests all properties with the given testing parameters, and returns
   *  the test results.
   */
  def checkProperties(prms: Test.Params): Map[String,Test.Stats] =
    checkProperties(prms, (n,r,s,d) => (), (n,s) => ())

  /** Tests all properties with the given testing parameters, and returns
   *  the test results. <code>f</code> is a function which is called each
   *  time a property is evaluted. <code>g</code> is a function called each
   *  time a property has been fully tested.
   */
  def checkProperties(prms: Test.Params, f: TestsInspector, g: TestStatsInspector
  ): Map[String,Test.Stats] = properties transform { case (pName,p) =>
    val stats = Test.check(prms,p,f(pName,_,_,_))
    g(pName,stats)
    stats
  }

  /** Tests all properties with default testing parameters, and returns
   *  the test results. The results are also printed on the console during
   *  testing.
   */
  def checkProperties(): Map[String,Test.Stats] =
  {
    def printTmp(pn: String, res: Option[Prop.Result], succ: Int, disc: Int) = {
      if(disc > 0)
        Console.printf("\r{3}: Passed {0} tests; {1} discarded",succ,disc,pn)
      else
        Console.printf("\r{1}: Passed {0} tests",succ,pn)
      Console.flush
    }

    def printStats(pName: String, stats: Test.Stats) = stats.result match {
      case Test.GenException(e) =>
        Console.printf("\r{1}: *** Exception raised when generating arguments:\n{0}               \n\n",
          e, pName)
      case Test.PropException(args,shrinks,e) =>
        Console.printf("\r{0}: *** Exception raised when evaluating property                        \n",
          pName)
        if(shrinks > 0)
          Console.printf("The arguments that caused the failure was (after {1} shrinks):\n{0}\n\n",
            args, shrinks)
        else 
          Console.printf("The arguments that caused the failure was:\n{0}\n\n", args)
        Console.printf("The raised exception was:\n{0}\n\n", e)
      case Test.Failed(args,shrinks) =>
        Console.printf("\r{1}: *** Failed after {0} successful tests                                \n",
          stats.succeeded, pName)
        if(shrinks > 0)
          Console.printf("The arguments that caused the failure was (after {1} shrinks):\n{0}\n\n",
            args, shrinks)
        else 
          Console.printf("The arguments that caused the failure was:\n{0}\n\n", args)
      case Test.Exhausted() =>
        Console.printf("\r{2}: *** Gave up, after only {1} passed tests. {0} tests were discarded.\n\n",
          stats.discarded, stats.succeeded, pName)
      case Test.Passed() =>
        Console.printf("\r{1}: +++ OK, passed {0} tests.                                          \n\n",
          stats.succeeded, pName)
    }

    checkProperties(Test.defaultParams,printTmp,printStats)
  }

  private def propToTestCase(pn: String, p: Prop): TestCase = new TestCase(pn) {

    protected def runTest() = {
      val stats = Test.check(Test.defaultParams,p)
      stats.result match {
        case Test.GenException(e) => fail(
          " Exception raised when generating arguments.\n" +
          "The raised exception was:\n"+e.toString+"\n"
        )
        case Test.PropException(args,shrinks,e) => fail(
          " Exception raised when evaluating property.\n\n" +
          (if(shrinks > 0)
            "The arguments that caused the failure was (after " +
            shrinks.toString+" shrinks):\n"+args.toString+"\n\n"
          else
            "The arguments that caused the failure was:\n"+args.toString+"\n\n")
          + "The raised exception was:\n"+e.toString+"\n"
        )
        case Test.Failed(args,shrinks) => fail(
          " Property failed after " + stats.succeeded.toString +
          " successful tests.\n" +
          (if(shrinks > 0)
            "The arguments that caused the failure was (after " +
            shrinks.toString+" shrinks):\n"+args.toString+"\n\n"
          else
            "The arguments that caused the failure was:\n"+args.toString+"\n\n")
        )
        case Test.Exhausted() => fail(
          " Gave up after only " + stats.succeeded.toString + " tests. " +
          stats.discarded.toString + " tests were discarded."
        )
        case Test.Passed() => ()
      }
    }

  }

  /** Returns all properties as SUnit.TestCase instances, which can added to
   *  a SUnit.TestSuite.
   */
  def testCases: List[TestCase] =
    (properties map {case (pn,p) => propToTestCase(pn,p)}).toList

  /** Returns all properties combined into a single property, that holds
   *  when all properties hold
   */
  def allProperties: Prop = Prop.all((properties map (_._2)).toList)

}
