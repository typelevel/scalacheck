/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2008 Rickard Nilsson. All rights reserved.          **
**  http://code.google.com/p/scalacheck/                                   **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*-------------------------------------------------------------------------*/

package org.scalacheck

import Util._

object Test {

  // Types

  /** Test parameters */
  case class Params(minSuccessfulTests: Int, maxDiscardedTests: Int,
    minSize: Int, maxSize: Int, rand: RandomGenerator)

  /** Test statistics */
  case class Stats(result: Result, succeeded: Int, discarded: Int)

  /** Test result */
  abstract sealed class Result { def passed = false }

  /** The property test passed */
  case class Passed extends Result { override def passed = true }

  /** The property was proved wrong with the given concrete arguments.  */
  case class Failed(args: List[Arg]) extends Result

  /** The property test was exhausted, it wasn't possible to generate enough
   *  concrete arguments satisfying the preconditions to get enough passing
   *  property evaluations. */
  case class Exhausted extends Result

  /** An exception was raised when trying to evaluate the property with the
   *  given concrete arguments. */
  case class PropException(args: List[Arg], e: Throwable) extends Result

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

  import ConsoleReporter._

  /** Tests a property and prints results to the console */
  def check(p: Prop): Stats = testReport(check(defaultParams, p, propReport))

}
