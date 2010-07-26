/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2010 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*-------------------------------------------------------------------------*/

package org.scalacheck


object Test {

  import util.FreqMap
  import scala.collection.immutable
  import Prop.FM
  import util.CmdLineParser
  import ConsoleReporter.{propReport, testReport}

  private def secure[T](x: => T): Either[T,Throwable] =
    try { Left(x) } catch { case e => Right(e) }


  // Types

  /** Test parameters */
  case class Params(
    minSuccessfulTests: Int = 100, 
    maxDiscardedTests: Int = 500,
    minSize: Int = 0, 
    maxSize: Int = Gen.Params().size, 
    rng: java.util.Random = Gen.Params().rng,
    workers: Int = 1,
    propCallback: PropCallback = (n,w,s,d) => (),
    testCallback: TestCallback = (n,r) => ()
  )

  /** Test statistics */
  case class Result(status: Status, succeeded: Int, discarded: Int, freqMap: FM) {
    def passed = status match {
      case Passed => true
      case Proved(_) => true
      case _ => false
    }
  }

  /** Test status */
  sealed trait Status

  /** ScalaCheck found enough cases for which the property holds, so the
   *  property is considered correct. (It is not proved correct, though). */
  case object Passed extends Status

  /** ScalaCheck managed to prove the property correct */
  sealed case class Proved(args: Prop.Args) extends Status

  /** The property was proved wrong with the given concrete arguments.  */
  sealed case class Failed(args: Prop.Args, labels: Set[String]) extends Status

  /** The property test was exhausted, it wasn't possible to generate enough
   *  concrete arguments satisfying the preconditions to get enough passing
   *  property evaluations. */
  case object Exhausted extends Status

  /** An exception was raised when trying to evaluate the property with the
   *  given concrete arguments. */
  sealed case class PropException(args: Prop.Args, e: Throwable, 
    labels: Set[String]) extends Status

  /** An exception was raised when trying to generate concrete arguments
   *  for evaluating the property. */
  sealed case class GenException(e: Throwable) extends Status

  /** Property evaluation callback. Takes number of passed and
   *  discarded tests, respectively */
  @deprecated("(v1.8)")
  type PropEvalCallback = (Int,Int) => Unit

  /** Property evaluation callback. Takes property name, and number of passed
   *  and discarded tests, respectively */
  @deprecated("(v1.8)")
  type NamedPropEvalCallback = (String,Int,Int) => Unit

  /** Test callback. Takes property name, and test results. */
  @deprecated("(v1.8)")
  type TestResCallback = (String,Result) => Unit

  type PropCallback = (String,Int,Int,Int) => Unit
  type TestCallback = (String,Result) => Unit

  /** Default testing parameters
   *  @deprecated Use <code>Test.Params()</code> instead */
  @deprecated("Use Test.Params() instead")
  val defaultParams = Params()
  
  private def assertParams(prms: Params) = {
    import prms._
    if(
      minSuccessfulTests <= 0 || 
      maxDiscardedTests < 0 || 
      minSize < 0 || 
      maxSize < minSize ||
      workers <= 0
    ) throw new IllegalArgumentException("Invalid test parameters")
  }

  private[scalacheck] lazy val cmdLineParser = new CmdLineParser {
    object OptMinSuccess extends IntOpt {
      val default = Test.Params().minSuccessfulTests
      val names = Set("minSuccessfulTests", "s")
      val help = "Number of tests that must succeed in order to pass a property"
    }
    object OptMaxDiscarded extends IntOpt {
      val default = Test.Params().maxDiscardedTests
      val names = Set("maxDiscardedTests", "d")
      val help =
        "Number of tests that can be discarded before ScalaCheck stops " +
        "testing a property"
    }
    object OptMinSize extends IntOpt {
      val default = Test.Params().minSize
      val names = Set("minSize", "n")
      val help = "Minimum data generation size"
    }
    object OptMaxSize extends IntOpt {
      val default = Test.Params().maxSize
      val names = Set("maxSize", "x")
      val help = "Maximum data generation size"
    }
    object OptWorkers extends IntOpt {
      val default = Test.Params().workers
      val names = Set("workers", "w")
      val help = "Number of threads to execute in parallel for testing"
    }

    val opts = Set[Opt[_]](
      OptMinSuccess, OptMaxDiscarded, OptMinSize,
      OptMaxSize, OptWorkers
    )

    def parseParams(args: Array[String]) = parseArgs(args) {
      optMap => Test.Params(
        optMap(OptMinSuccess),
        optMap(OptMaxDiscarded),
        optMap(OptMinSize),
        optMap(OptMaxSize),
        Test.Params().rng,
        optMap(OptWorkers)
      )
    }
  }

  /** Tests a property with the given testing parameters, and returns
   *  the test results. */
  def check(prms: Params, p: Prop): Result = {
    import prms._
    import actors.Futures.future

    assertParams(prms)
    if(workers > 1)
      assert(!p.isInstanceOf[Commands], "Commands cannot be checked multi-threaded")

    val iterations = minSuccessfulTests / workers
    val sizeStep = (maxSize-minSize) / (minSuccessfulTests: Float)
    var stop = false

    def worker(startSize: Float) = future {
      var n = 0
      var d = 0
      var size = startSize
      var res: Result = null
      var fm = FreqMap.empty[immutable.Set[Any]]
      while(!stop && res == null && n < iterations) {
        val propPrms = Prop.Params(Gen.Params(size.round, prms.rng), fm)
        secure(p(propPrms)) match {
          case Right(e) => res = 
            Result(GenException(e), n, d, FreqMap.empty[immutable.Set[Any]])
          case Left(propRes) =>
            fm =
              if(propRes.collected.isEmpty) fm
              else fm + propRes.collected
            propRes.status match {
              case Prop.Undecided =>
                d += 1
                propCallback("", 0, n, d)
                if(d >= maxDiscardedTests) res = Result(Exhausted, n, d, fm)
              case Prop.True => 
                n += 1
                propCallback("", 0, n, d)
              case Prop.Proof => 
                n += 1
                res = Result(Proved(propRes.args), n, d, fm)
              case Prop.False => res = 
                Result(Failed(propRes.args, propRes.labels), n, d, fm)
              case Prop.Exception(e) => res =
                Result(PropException(propRes.args, e, propRes.labels), n, d, fm)
            }
        }
        size += sizeStep
      }
      if(res != null) stop = true
      else res = Result(Passed, n, d, fm)
      res
    }

    def mergeResults(r1: () => Result, r2: () => Result) = r1() match {
      case Result(Passed, s1, d1, fm1) => r2() match {
        case Result(Passed, s2, d2, fm2) if d1+d2 >= maxDiscardedTests =>
          () => Result(Exhausted, s1+s2, d1+d2, fm1++fm2)
        case Result(st, s2, d2, fm2) => 
          () => Result(st, s1+s2, d1+d2, fm1++fm2)
      }
      case r => () => r
    }

    val results = for(i <- 0 until workers) yield worker(i*sizeStep)
    val r = results.reduceLeft(mergeResults)()
    stop = true
    results foreach (_.apply())
    prms.testCallback("", r)
    r
  }

  def checkProperties(prms: Params, ps: Properties): Seq[(String,Result)] =
    ps.properties.map { case (name,p) =>
      val propCallb: PropCallback = (n,w,s,d) => prms.propCallback(name,w,s,d)
      val testCallb: TestCallback = (n,r) => prms.testCallback(name,r)
      val newPrms = prms copy (propCallback = propCallb, testCallback = testCallb)
      val res = check(newPrms, p)
      (name,res)
    }


  // Deprecated methods //

  /** @deprecated (v1.8) Use <code>check(prms.copy(propCallback = myCallback), p)</code> instead. */
  @deprecated("(v1.8) Use check(prms.copy(propCallback = myCallback), p) instead")
  def check(prms: Params, p: Prop, propCallb: PropEvalCallback): Result =
    check(prms copy (propCallback = (n,w,s,d) => propCallb(s,d)), p)

  /** Tests a property and prints results to the console. The
   *  <code>maxDiscarded</code> parameter specifies how many
   *  discarded tests that should be allowed before ScalaCheck
   *  @deprecated (v1.8) Use <code>check(Params(maxDiscardedTests = n), p)</code> instead. */
  @deprecated("(v1.8) Use check(Params(maxDiscardedTests = n), p) instead.")
  def check(p: Prop, maxDiscarded: Int): Result = 
    check(Params(maxDiscardedTests = maxDiscarded, testCallback = testReport), p)

  /** Tests a property and prints results to the console
   *  @deprecated (v1.8) Use <code>check(Params(propCallback = ConsoleReporter.propReport), p)</code> instead. */
  @deprecated("(v1.8) Use check(Params(propCallback = ConsoleReporter.propReport, testCallback = ConsoleReporter.testReport), p) instead.")
  def check(p: Prop): Result = check(Params(propCallback = propReport, testCallback = testReport), p)

  /** Tests all properties with the given testing parameters, and returns
   *  the test results. <code>f</code> is a function which is called each
   *  time a property is evaluted. <code>g</code> is a function called each
   *  time a property has been fully tested.
   *  @deprecated (v1.8) Use <code>checkProperties(prms.copy(propCallback = myPropCallback, testCallback = myTestCallback), ps)</code> instead. */
  @deprecated("(v1.8) Use checkProperties(prms.copy(propCallBack = myPropCallback, testCallback = myTestCallback), ps) instead.")
  def checkProperties(ps: Properties, prms: Params,
    propCallb: NamedPropEvalCallback, testCallb: TestResCallback
  ): Seq[(String,Result)] = checkProperties(
    prms.copy(propCallback = (n,w,s,d) => propCallb(n,s,d), testCallback = testCallb),
    ps
  )

  /** Tests all properties with the given testing parameters, and returns
   *  the test results. 
   *  @deprecated (v1.8) Use checkProperties(prms, ps) instead */
  @deprecated("(v1.8) Use checkProperties(prms, ps) instead")
  def checkProperties(ps: Properties, prms: Params): Seq[(String,Result)] =
    checkProperties(ps, prms, (n,s,d) => (), (n,s) => ())

  /** Tests all properties with default testing parameters, and returns
   *  the test results. The results are also printed on the console during
   *  testing.
   *  @deprecated (v1.8) Use <code>checkProperties(Params(), ps)</code> instead. */
  @deprecated("(v1.8) Use checkProperties(Params(), ps) instead.")
  def checkProperties(ps: Properties): Seq[(String,Result)] =
    checkProperties(Params(), ps)
}
