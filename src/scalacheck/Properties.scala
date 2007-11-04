package scalacheck

/** Represents a collection of properties, with convenient methods
 *  for checking all properties at once. Properties are added to this
 *  collection through the <code>specify</code> methods. */
trait Properties {

  import scala.collection.Map
  import scala.testing.SUnit.TestCase

  private var properties = scala.collection.immutable.Map.empty[String, Prop]

  /** Adds a property to this property collection */
  def specify(propName: String, prop: => Prop) =
    addProp(propName, Prop.property(prop))

  /** Adds a property to this property collection */
  def specify[A1,P] (
    propName: String, f: A1 => P)(implicit
    p: P => Prop,
    a1: Arb[A1] => Arbitrary[A1]
  ): Unit = addProp(propName,Prop.property(f))

  /** Adds a property to this property collection */
  def specify[A1,A2,P] (
    propName: String, f: (A1,A2) => P)(implicit
    p: P => Prop,
    a1: Arb[A1] => Arbitrary[A1],
    a2: Arb[A2] => Arbitrary[A2]
  ): Unit = addProp(propName,Prop.property(f))

  /** Adds a property to this property collection */
  def specify[A1,A2,A3,P] (
    propName: String, f: (A1,A2,A3) => P)(implicit
    p: P => Prop,
    a1: Arb[A1] => Arbitrary[A1],
    a2: Arb[A2] => Arbitrary[A2],
    a3: Arb[A3] => Arbitrary[A3]
  ): Unit = addProp(propName,Prop.property(f))

  /** Adds a property to this property collection */
  def specify[A1,A2,A3,A4,P] (
    propName: String, f: (A1,A2,A3,A4) => P)(implicit
    p: P => Prop,
    a1: Arb[A1] => Arbitrary[A1],
    a2: Arb[A2] => Arbitrary[A2],
    a3: Arb[A3] => Arbitrary[A3],
    a4: Arb[A4] => Arbitrary[A4]
  ): Unit = addProp(propName,Prop.property(f))

  /** Adds a property to this property collection */
  def specify[A1,A2,A3,A4,A5,P] (
    propName: String, f: (A1,A2,A3,A4,A5) => P)(implicit
    p: P => Prop,
    a1: Arb[A1] => Arbitrary[A1],
    a2: Arb[A2] => Arbitrary[A2],
    a3: Arb[A3] => Arbitrary[A3],
    a4: Arb[A5] => Arbitrary[A5],
    a5: Arb[A4] => Arbitrary[A4]
  ): Unit = addProp(propName,Prop.property(f))

  /** Adds a property to this property collection */
  def specify[A1,A2,A3,A4,A5,A6,P] (
    propName: String, f: (A1,A2,A3,A4,A5,A6) => P)(implicit
    p: P => Prop,
    a1: Arb[A1] => Arbitrary[A1],
    a2: Arb[A2] => Arbitrary[A2],
    a3: Arb[A3] => Arbitrary[A3],
    a4: Arb[A4] => Arbitrary[A4],
    a5: Arb[A5] => Arbitrary[A5],
    a6: Arb[A6] => Arbitrary[A6]
  ): Unit = addProp(propName,Prop.property(f))

  private def addProp(propName: String, prop: Prop) =
    properties = properties.update(propName, prop)

  type NamedPropEvalCallback = (String,Option[Prop.Result],Int,Int) => Unit
  type TestStatsCallback = (String,Test.Stats) => Unit

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
  def checkProperties(prms: Test.Params, propCallback: NamedPropEvalCallback,
    testCallback: TestStatsCallback
  ): Map[String,Test.Stats] = properties transform { case (pName,p) =>
    val stats = Test.check(prms,p,propCallback(pName,_,_,_))
    testCallback(pName,stats)
    stats
  }

  import ConsoleReporter._

  /** Tests all properties with default testing parameters, and returns
   *  the test results. The results are also printed on the console during
   *  testing. */
  def checkProperties(): Map[String,Test.Stats] =
    checkProperties(Test.defaultParams, propReport, testReport)

  private def propToTestCase(pn: String, p: Prop): TestCase = new TestCase(pn) {
    protected def runTest() = {
      val stats = Test.check(Test.defaultParams,p)
      if(!stats.result.passed) fail(prettyTestStats(stats))
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
