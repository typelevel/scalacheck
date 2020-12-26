/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import Prop.Arg
import java.lang.Math
import org.scalacheck.util.{FreqMap, CmdLineParser, ConsoleReporter}
import scala.util.{Success, Failure}
import scala.util.matching.Regex

object Test {


  /** Test parameters used by the check methods. Default
   *  parameters are defined by [[Test.Parameters.default]]. */
  sealed abstract class Parameters { outer =>
    /** The minimum number of tests that must succeed for ScalaCheck to
     *  consider a property passed. */
    val minSuccessfulTests: Int

    /** Create a copy of this [[Test.Parameters]] instance with
     *  [[Test.Parameters.minSuccessfulTests]] set to the specified value. */
    def withMinSuccessfulTests(minSuccessfulTests: Int): Parameters =
      cpy(minSuccessfulTests0 = minSuccessfulTests)

    /** The starting size given as parameter to the generators. */
    val minSize: Int

    /** Create a copy of this [[Test.Parameters]] instance with
     *  [[Test.Parameters.minSize]] set to the specified value. */
    def withMinSize(minSize: Int): Parameters =
      cpy(minSize0 = minSize)

    /** The maximum size given as parameter to the generators. */
    val maxSize: Int

    /** Create a copy of this [[Test.Parameters]] instance with
     *  [[Test.Parameters.maxSize]] set to the specified value. */
    def withMaxSize(maxSize: Int): Parameters =
      cpy(maxSize0 = maxSize)

    /** The number of tests to run in parallel. */
    val workers: Int

    /** Create a copy of this [[Test.Parameters]] instance with
     *  [[Test.Parameters.workers]] set to the specified value. */
    def withWorkers(workers: Int): Parameters =
      cpy(workers0 = workers)

    /** A callback that ScalaCheck calls each time a test is executed. */
    val testCallback: TestCallback

    /** Create a copy of this [[Test.Parameters]] instance with
     *  [[Test.Parameters.testCallback]] set to the specified value. */
    def withTestCallback(testCallback: TestCallback): Parameters =
      cpy(testCallback0 = testCallback)

    /** The maximum ratio between discarded and passed tests allowed before
     *  ScalaCheck gives up and discards the whole property (with status
     *  [[Test.Exhausted]]). Additionally, ScalaCheck will always allow
     *  at least `minSuccessfulTests * maxDiscardRatio` discarded tests, so the
     *  resulting discard ratio might be higher than `maxDiscardRatio`. */
    val maxDiscardRatio: Float

    /** Create a copy of this [[Test.Parameters]] instance with
     *  [[Test.Parameters.maxDiscardRatio]] set to the specified value. */
    def withMaxDiscardRatio(maxDiscardRatio: Float): Parameters =
      cpy(maxDiscardRatio0 = maxDiscardRatio)

    /** A custom class loader that should be used during test execution. */
    val customClassLoader: Option[ClassLoader]

    /** Create a copy of this [[Test.Parameters]] instance with
     *  [[Test.Parameters.customClassLoader]] set to the specified value. */
    def withCustomClassLoader(customClassLoader: Option[ClassLoader]): Parameters =
      cpy(customClassLoader0 = customClassLoader)

    /** An optional regular expression to filter properties on. */
    val propFilter: Option[String]

    /** Create a copy of this [[Test.Parameters]] instance with
     *  [[Test.Parameters.propFilter]] set to the specified regular expression
     *  filter. */
    def withPropFilter(propFilter: Option[String]): Parameters =
      cpy(propFilter0 = propFilter)

    /** Initial seed to use for testing. */
    val initialSeed: Option[rng.Seed]

    /** Set initial seed to use. */
    def withInitialSeed(o: Option[rng.Seed]): Parameters =
      cpy(initialSeed0 = o)

    /** Set initial seed to use. */
    def withInitialSeed(seed: rng.Seed): Parameters =
      cpy(initialSeed0 = Some(seed))

    /** Set initial seed as long integer. */
    def withInitialSeed(n: Long): Parameters =
      cpy(initialSeed0 = Some(rng.Seed(n)))

    /** Don't set an initial seed. */
    def withNoInitialSeed: Parameters =
      cpy(initialSeed0 = None)

    /** Use legacy shrinking. */
    val useLegacyShrinking: Boolean = true

    /** Disable legacy shrinking. */
    def disableLegacyShrinking: Parameters =
      withLegacyShrinking(false)

    /** Enable legacy shrinking. */
    def enableLegacyShrinking: Parameters =
      withLegacyShrinking(true)

    /** Set legacy shrinking. */
    def withLegacyShrinking(b: Boolean): Parameters =
      cpy(useLegacyShrinking0 = b)

    override def toString: String = {
      val sb = new StringBuilder
      sb.append("Parameters(")
      sb.append(s"minSuccessfulTests=$minSuccessfulTests, ")
      sb.append(s"minSize=$minSize, ")
      sb.append(s"maxSize=$maxSize, ")
      sb.append(s"workers=$workers, ")
      sb.append(s"testCallback=$testCallback, ")
      sb.append(s"maxDiscardRatio=$maxDiscardRatio, ")
      sb.append(s"customClassLoader=$customClassLoader, ")
      sb.append(s"propFilter=$propFilter, ")
      sb.append(s"initialSeed=$initialSeed, ")
      sb.append(s"useLegacyShrinking=$useLegacyShrinking)")
      sb.toString
    }

    /** Copy constructor with named default arguments */
    private[this] def cpy(
      minSuccessfulTests0: Int = outer.minSuccessfulTests,
      minSize0: Int = outer.minSize,
      maxSize0: Int = outer.maxSize,
      workers0: Int = outer.workers,
      testCallback0: TestCallback = outer.testCallback,
      maxDiscardRatio0: Float = outer.maxDiscardRatio,
      customClassLoader0: Option[ClassLoader] = outer.customClassLoader,
      propFilter0: Option[String] = outer.propFilter,
      initialSeed0: Option[rng.Seed] = outer.initialSeed,
      useLegacyShrinking0: Boolean = outer.useLegacyShrinking
    ): Parameters =
      new Parameters {
        val minSuccessfulTests: Int = minSuccessfulTests0
        val minSize: Int = minSize0
        val maxSize: Int = maxSize0
        val workers: Int = workers0
        val testCallback: TestCallback = testCallback0
        val maxDiscardRatio: Float = maxDiscardRatio0
        val customClassLoader: Option[ClassLoader] = customClassLoader0
        val propFilter: Option[String] = propFilter0
        val initialSeed: Option[rng.Seed] = initialSeed0
        override val useLegacyShrinking: Boolean = useLegacyShrinking0
      }

    // no longer used, but preserved for binary compatibility
    @deprecated("cp is deprecated. use cpy.", "1.14.1")
    private case class cp(
      minSuccessfulTests: Int = minSuccessfulTests,
      minSize: Int = minSize,
      maxSize: Int = maxSize,
      workers: Int = workers,
      testCallback: TestCallback = testCallback,
      maxDiscardRatio: Float = maxDiscardRatio,
      customClassLoader: Option[ClassLoader] = customClassLoader,
      propFilter: Option[String] = propFilter,
      initialSeed: Option[rng.Seed] = initialSeed
    ) extends Parameters
  }

  /** Test parameters used by the check methods. Default
   *  parameters are defined by [[Test.Parameters.default]]. */
  object Parameters {
    /** Default test parameters. Can be overridden if you need to
     *  tweak the parameters:
     *
     *  {{{
     *  val myParams = Parameters.default
     *    .withMinSuccessfulTests(600)
     *    .withMaxDiscardRatio(8)
     *  }}} */
    val default: Parameters = new Parameters {
      val minSuccessfulTests: Int = 100
      val minSize: Int = 0
      val maxSize: Int = Gen.Parameters.default.size
      val workers: Int = 1
      val testCallback: TestCallback = new TestCallback {}
      val maxDiscardRatio: Float = 5
      val customClassLoader: Option[ClassLoader] = None
      val propFilter = None
      val initialSeed: Option[rng.Seed] = None
    }

    /** Verbose console reporter test parameters instance. */
    val defaultVerbose: Parameters = default.withTestCallback(ConsoleReporter(2))
  }

  /** Test statistics */
  final case class Result(
    status: Status,
    succeeded: Int,
    discarded: Int,
    freqMap: FreqMap[Set[Any]],
    time: Long = 0
  ) {
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
  sealed case class Proved(args: List[Arg[Any]]) extends Status

  /** The property was proved wrong with the given concrete arguments.  */
  sealed case class Failed(args: List[Arg[Any]], labels: Set[String]) extends Status

  /** The property test was exhausted, it wasn't possible to generate enough
   *  concrete arguments satisfying the preconditions to get enough passing
   *  property evaluations. */
  case object Exhausted extends Status

  /** An exception was raised when trying to evaluate the property with the
   *  given concrete arguments. If an exception was raised before or during
   *  argument generation, the argument list will be empty. */
  sealed case class PropException(args: List[Arg[Any]], e: Throwable,
    labels: Set[String]) extends Status

  trait TestCallback { self =>
    /** Called each time a property is evaluated */
    def onPropEval(name: String, threadIdx: Int, succeeded: Int,
      discarded: Int): Unit = ()

    /** Called whenever a property has finished testing */
    def onTestResult(name: String, result: Result): Unit = ()

    def chain(testCallback: TestCallback): TestCallback = new TestCallback {
      override def onPropEval(name: String, threadIdx: Int,
        succeeded: Int, discarded: Int
      ): Unit = {
        self.onPropEval(name,threadIdx,succeeded,discarded)
        testCallback.onPropEval(name,threadIdx,succeeded,discarded)
      }

      override def onTestResult(name: String, result: Result): Unit = {
        self.onTestResult(name,result)
        testCallback.onTestResult(name,result)
      }
    }
  }

  private def assertParams(prms: Parameters) = {
    if (prms.minSuccessfulTests <= 0)
      throw new IllegalArgumentException(
        s"Invalid test parameter: minSuccessfulTests (${prms.minSuccessfulTests}) <= 0")
    else if (prms.maxDiscardRatio <= 0)
      throw new IllegalArgumentException(
        s"Invalid test parameter: maxDiscardRatio (${prms.maxDiscardRatio}) <= 0")
    else if (prms.minSize < 0)
      throw new IllegalArgumentException(
        s"Invalid test parameter: minSize (${prms.minSize}) < 0")
    else if (prms.maxSize < prms.minSize)
      throw new IllegalArgumentException(
        s"Invalid test parameter: maxSize (${prms.maxSize}) < minSize (${prms.minSize})")
    else if (prms.workers <= 0)
      throw new IllegalArgumentException(
        s"Invalid test parameter: workers (${prms.workers}) <= 0")
  }

  private[scalacheck] object CmdLineParser extends CmdLineParser {
    object OptMinSuccess extends IntOpt {
      val default = Parameters.default.minSuccessfulTests
      val names = Set("minSuccessfulTests", "s")
      val help = "Number of tests that must succeed in order to pass a property"
    }
    object OptMaxDiscardRatio extends FloatOpt {
      val default = Parameters.default.maxDiscardRatio
      val names = Set("maxDiscardRatio", "r")
      val help =
        "The maximum ratio between discarded and succeeded tests " +
        "allowed before ScalaCheck stops testing a property. At " +
        "least minSuccessfulTests will always be tested, though."
    }
    object OptMinSize extends IntOpt {
      val default = Parameters.default.minSize
      val names = Set("minSize", "n")
      val help = "Minimum data generation size"
    }
    object OptMaxSize extends IntOpt {
      val default = Parameters.default.maxSize
      val names = Set("maxSize", "x")
      val help = "Maximum data generation size"
    }
    object OptWorkers extends IntOpt {
      val default = Parameters.default.workers
      val names = Set("workers", "w")
      val help = "Number of threads to execute in parallel for testing"
    }
    object OptVerbosity extends IntOpt {
      val default = 1
      val names = Set("verbosity", "v")
      val help = "Verbosity level"
    }

    object OptPropFilter extends OpStrOpt {
      val default = Parameters.default.propFilter
      val names = Set("propFilter", "f")
      val help = "Regular expression to filter properties on"
    }

    object OptInitialSeed extends OpStrOpt {
      val default = None
      val names = Set("initialSeed")
      val help = "Use Base-64 seed for all properties"
    }

    object OptDisableLegacyShrinking extends Flag {
      val default = ()
      val names = Set("disableLegacyShrinking")
      val help = "Disable legacy shrinking using Shrink instances"
    }

    val opts = Set[Opt[_]](
      OptMinSuccess, OptMaxDiscardRatio, OptMinSize,
      OptMaxSize, OptWorkers, OptVerbosity,
      OptPropFilter, OptInitialSeed, OptDisableLegacyShrinking
    )

    def parseParams(args: Array[String]): (Parameters => Parameters, List[String]) = {
      val (optMap, us) = parseArgs(args)
      val minSuccess0: Int = optMap(OptMinSuccess)
      val minSize0: Int = optMap(OptMinSize)
      val maxSize0: Int = optMap(OptMaxSize)
      val workers0: Int = optMap(OptWorkers)
      val verbosity0 = optMap(OptVerbosity)
      val discardRatio0: Float = optMap(OptMaxDiscardRatio)
      val propFilter0: Option[String] = optMap(OptPropFilter)
      val initialSeed0: Option[rng.Seed] =
        optMap(OptInitialSeed).flatMap { str =>
          rng.Seed.fromBase64(str) match {
            case Success(seed) =>
              Some(seed)
            case Failure(_) =>
              println(s"WARNING: ignoring invalid Base-64 seed ($str)")
              None
          }
        }

      val useLegacyShrinking0: Boolean = !optMap(OptDisableLegacyShrinking)
      val params = { (p: Parameters) =>
        p.withMinSuccessfulTests(minSuccess0)
          .withMinSize(minSize0)
          .withMaxSize(maxSize0)
          .withWorkers(workers0)
          .withTestCallback(ConsoleReporter(verbosity0))
          .withMaxDiscardRatio(discardRatio0)
          .withPropFilter(propFilter0)
          .withInitialSeed(initialSeed0)
          .withLegacyShrinking(useLegacyShrinking0)
      }
      (params, us)
    }
  }

  /** Tests a property with parameters that are calculated by applying
   *  the provided function to [[Test.Parameters.default]].
   *  Example use:
   *
   *  {{{
   *  Test.check(p) { _.
   *    withMinSuccessfulTests(80000).
   *    withWorkers(4)
   *  }
   *  }}}
   */
  def check(p: Prop)(f: Parameters => Parameters): Result =
    check(f(Parameters.default), p)

  /** Tests a property with the given testing parameters, and returns
   *  the test results. */
  def check(params: Parameters, p: Prop): Result = {
    assertParams(params)

    val iterations = Math.ceil(params.minSuccessfulTests / params.workers.toDouble)
    val sizeStep = (params.maxSize - params.minSize) / (iterations * params.workers)
    var stop = false

    def workerFun(workerIdx: Int): Result = {
      var n = 0  // passed tests
      var d = 0  // discarded tests
      var res: Result = null
      var fm = FreqMap.empty[Set[Any]]

      def isExhausted = d > params.minSuccessfulTests * params.maxDiscardRatio

      var seed = {
        val seed0 = params.initialSeed.getOrElse(rng.Seed.random())
        if (workerIdx == 0) seed0 else seed0.reseed(workerIdx.toLong)
      }

      while(!stop && res == null && n < iterations) {

        val count = workerIdx + (params.workers * (n + d))
        val size = params.minSize.toDouble + (sizeStep * count)
        val genPrms = Gen.Parameters.default
          .withLegacyShrinking(params.useLegacyShrinking)
          .withInitialSeed(Some(seed))
          .withSize(size.round.toInt)

        seed = seed.slide

        val propRes = p(genPrms)
        if (propRes.collected.nonEmpty) {
          fm = fm + propRes.collected
        }

        propRes.status match {
          case Prop.Undecided =>
            d += 1
            params.testCallback.onPropEval("", workerIdx, n, d)
            if (isExhausted) res = Result(Exhausted, n, d, fm)
          case Prop.True =>
            n += 1
            params.testCallback.onPropEval("", workerIdx, n, d)
          case Prop.Proof =>
            n += 1
            res = Result(Proved(propRes.args), n, d, fm)
            stop = true
          case Prop.False =>
            res = Result(Failed(propRes.args,propRes.labels), n, d, fm)
            stop = true
          case Prop.Exception(e) =>
            res = Result(PropException(propRes.args,e,propRes.labels), n, d, fm)
            stop = true
        }
      }
      if (res == null) {
        if (isExhausted) Result(Exhausted, n, d, fm)
        else Result(Passed, n, d, fm)
      } else res
    }

    val t0 = System.nanoTime()
    val r = Platform.runWorkers(params, workerFun, () => stop = true)
    val millis = (System.nanoTime() - t0) / 1000000L
    val timedRes = r.copy(time = millis)
    params.testCallback.onTestResult("", timedRes)
    timedRes
  }

  /** Returns the result of filtering a property name by a supplied regular expression.
    *
    *  @param propertyName The name of the property to be filtered.
    *  @param regex The regular expression to filter the property name by.
    *  @return true if the regular expression matches the property name, false if not.
    */
  def matchPropFilter(propertyName: String, regex: Regex): Boolean = {
    regex.findFirstIn(propertyName).isDefined
  }

  private def buildPredicate(o: Option[String]): String => Boolean =
    o match {
      case Some(expr) =>
        val regex = expr.r
        (s: String) => matchPropFilter(s, regex)
      case None =>
        (s: String) => true
    }

  /** Check a set of properties. */
  def checkProperties(prms: Parameters, ps: Properties): collection.Seq[(String, Result)] = {
    val params1 = ps.overrideParameters(prms)
    val isMatch = buildPredicate(params1.propFilter)
    val props = ps.properties.filter { case (name, _) => isMatch(name) }

    props.map { case (name, prop) =>
      val params2 = params1.withTestCallback(
        new TestCallback {
          override def onPropEval(n: String, t: Int, s: Int, d: Int) =
            params1.testCallback.onPropEval(name, t, s, d)
          override def onTestResult(n: String, r: Result) =
            params1.testCallback.onTestResult(name, r)
        })

      val res = Test.check(params2, prop)
      (name, res)
    }
  }
}
