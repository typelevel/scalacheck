/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2010 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*-------------------------------------------------------------------------*/

// vim: set ts=2 sw=2 et:

package org.scalacheck

import org.scalatools.testing._

class ScalaCheckFramework extends Framework {

  val name = "ScalaCheck"

  val tests = Array[TestFingerprint](
    new TestFingerprint {
      val superClassName = "org.scalacheck.Prop"
      val isModule = false
    },
    new TestFingerprint {
      val superClassName = "org.scalacheck.Properties"
      val isModule = true
    }
  )
 
  def testRunner(loader: ClassLoader,  loggers: Array[Logger]) = new Runner {

    private def asEvent(nr: (String, Test.Result)) = nr match { 
      case (n: String, r: Test.Result) => new Event {
        val testName = n
        val description = n
        val result = r.status match {
          case Test.Passed => Result.Success
          case _:Test.Proved => Result.Success
          case _:Test.Failed => Result.Failure
          case Test.Exhausted => Result.Skipped
          case _:Test.PropException | _:Test.GenException => Result.Error
        }
        val error = r.status match {
          case Test.PropException(_, e, _) => e
          case _:Test.Failed => new Exception(Pretty.pretty(r,Pretty.Params(0)))
          case _ => null
        }
      }
    }

    def run(testClassName: String, fingerprint: TestFingerprint, handler: EventHandler, args: Array[String]) {

      def loadClass = {
        if(fingerprint.isModule) {
          val obj = Class.forName(testClassName + "$", true, loader)
          obj.getField("MODULE$").get(null)
        } else {
          Class.forName(testClassName, true, loader).newInstance
        }
      }
      
      // TODO Loggers
      val testCallback = new Test.TestCallback {
        override def onPropEval(n: String, w: Int, s: Int, d: Int) = {}

        override def onTestResult(n: String, r: Test.Result) = {
          for (l <- loggers) {
            import Pretty._
            l.info(
              (if (r.passed) "+ " else "! ") + n + ": " + pretty(r, Params(0))
              )
          }
          handler.handle(asEvent((n,r)))
        }
      }

      import Test.cmdLineParser.{Success, NoSuccess}
      val prms = Test.cmdLineParser.parseParams(args) match {
        case Success(params, _) => 
          params copy (testCallback = testCallback)
        // TODO: Maybe handle this a bit better than throwing exception?
        case e: NoSuccess => throw new Exception(e.toString)
      }

      fingerprint.superClassName match {
        case "org.scalacheck.Prop" => 
          val p = loadClass.asInstanceOf[Prop]
          handler.handle(asEvent((testClassName, Test.check(prms, p))))
        case "org.scalacheck.Properties" =>
          val ps = loadClass.asInstanceOf[Properties]
          Test.checkProperties(prms, ps)
      }
    }

  }

}
