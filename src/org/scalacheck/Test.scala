/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2008 Rickard Nilsson. All rights reserved.          **
**  http://code.google.com/p/scalacheck/                                   **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*-------------------------------------------------------------------------*/

package org.scalacheck

object Test {

  import Util._
  import ConsoleReporter.{testReport, propReport}

  // Types

  /** Test parameters */
  case class Params(minSuccessfulTests: Int, maxDiscardedTests: Int,
    minSize: Int, maxSize: Int, rand: RandomGenerator)

  /** Test statistics */
  case class Stats(result: Result, succeeded: Int, discarded: Int)

  /** Test result */
  abstract sealed class Result { def passed = false }

  /** ScalaCheck found enough cases for which the property holds, so the
   *  property is considered correct. (It is not proved correct, though). */
  case object Passed extends Result { override def passed = true }

  /** ScalaCheck managed to prove the property correct */
  sealed case class Proved(args: List[Arg]) extends Result { 
    override def passed = true 
  }

  /** The property was proved wrong with the given concrete arguments.  */
  sealed case class Failed(args: List[Arg]) extends Result

  /** The property test was exhausted, it wasn't possible to generate enough
   *  concrete arguments satisfying the preconditions to get enough passing
   *  property evaluations. */
  case object Exhausted extends Result

  /** An exception was raised when trying to evaluate the property with the
   *  given concrete arguments. */
  sealed case class PropException(args: List[Arg], e: Throwable) extends Result

  /** An exception was raised when trying to generate concrete arguments
   *  for evaluating the property. */
  sealed case class GenException(e: Throwable) extends Result

  /** Property evaluation callback. Takes number of passed and
   *  discarded tests, respectively */
  type PropEvalCallback = (Int,Int) => Unit

  /** Default testing parameters */
  val defaultParams = Params(100,500,0,100,StdRand)


  // Testing functions

  /** Tests a property with the given testing parameters, and returns
   *  the test results. */
  def check(prms: Params, p: Prop): Stats = check(prms,p, (s,d) => ())

  /** Tests a property with the given testing parameters, and returns
   *  the test results. <code>propCallback</code> is a function which is
   *  called each time the property is evaluted. */
  def check(prms: Params, p: Prop, propCallback: PropEvalCallback): Stats =
  {
    def stats(s: Int, d: Int, sz: Float): Stats = {

      val size: Float = if(s == 0 && d == 0) prms.minSize else
        sz + ((prms.maxSize-sz)/(prms.minSuccessfulTests-s))

      secure(p(Gen.Params(size.round, prms.rand))) match {
        case Left(propRes) =>
          propRes match {
            case None =>
              if(d+1 >= prms.maxDiscardedTests) Stats(Exhausted,s,d+1)
              else { propCallback(s,d+1); stats(s,d+1,size) }
            case Some(Prop.Proof(as)) => Stats(Proved(as),s+1,d)
            case Some(_: Prop.True) =>
              if(s+1 >= prms.minSuccessfulTests) Stats(Passed,s+1,d)
              else { propCallback(s+1,d); stats(s+1,d,size) }
            case Some(Prop.False(as)) => Stats(Failed(as),s,d)
            case Some(Prop.Exception(as,e)) => Stats(PropException(as,e),s,d)
          }
        case Right(e) => Stats(GenException(e),s,d)
      }
    }

    stats(0,0,prms.minSize)
  }

  /** Tests a property with the given testing parameters, and returns
   *  the test results. <code>propCallback</code> is a function which is
   *  called each time the property is evaluted. Uses actors for parallel
   *  test execution, unless <code>workers</code> is zero.
   *  <code>worker</code> specifies how many working actors should be used.
   *  <code>wrkSize</code> specifies how many tests each worker should
   *  be scheduled with. */
  def check(prms: Params, p: Prop, propCallback: PropEvalCallback,
    workers: Int, wrkSize: Int
  ): Stats =
    if(workers <= 0) check(prms,p,propCallback)
    else {
      import scala.actors._
      import Actor._

      case class S(res: Result, s: Int, d: Int)

      val server = actor {
        var s = 0
        var d = 0
        var size: Float = prms.minSize
        var w = workers
        var stats: Stats = null
        loop { react {
          case 'wrkstop => w -= 1
          case 'get if w == 0 =>
            reply(stats)
            exit()
          case 'params => if(stats != null) reply() else {
            reply((s,d,size))
            size += wrkSize*((prms.maxSize-size)/(prms.minSuccessfulTests-s))
          }
          case S(res, sDelta, dDelta) if stats == null =>
            s += sDelta
            d += dDelta
            if(res != null) stats = Stats(res,s,d)
            else {
              if(s >= prms.minSuccessfulTests) stats = Stats(Passed,s,d)
              else if(d >= prms.maxDiscardedTests) stats = Stats(Exhausted,s,d)
              else propCallback(s,d)
            }
        }}
      }

      def worker = actor {
        var stop = false
        while(!stop) (server !? 'params) match {
          case (s: Int, d: Int, sz: Float) =>
            var s2 = s
            var d2 = d
            var size = sz
            var i = 0
            var res: Result = null
            while(res == null && i < wrkSize) {
              secure(p(Gen.Params(size.round, prms.rand))) match {
                case Left(propRes) => propRes match {
                  case None =>
                    d2 += 1
                    if(d2 >= prms.maxDiscardedTests) res = Exhausted
                  case Some(Prop.Proof(as)) =>
                    s2 += 1
                    res = Proved(as)
                  case Some(_: Prop.True) =>
                    s2 += 1
                    if(s2 >= prms.minSuccessfulTests) res = Passed
                  case Some(Prop.False(as)) => res = Failed(as)
                  case Some(Prop.Exception(as,e)) => res = PropException(as,e)
                }
                case Right(e) => res = GenException(e)
              }
              size += ((prms.maxSize-size)/(prms.minSuccessfulTests-s2))
              i += 1
            }
            server ! S(res,s2-s,d2-d)
          case _ => stop = true
        }
        server ! 'wrkstop
      }

      for(_ <- 1 to workers) worker
      (server !? 'get).asInstanceOf[Stats]
    }

  /** Tests a property and prints results to the console */
  def check(p: Prop): Stats = testReport(check(defaultParams, p, propReport))

}
