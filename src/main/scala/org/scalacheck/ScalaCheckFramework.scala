/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2009 Rickard Nilsson. All rights reserved.          **
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
        val result = r.status match {
          case Test.Passed => Result.Success
          case _:Test.Proved => Result.Success
          case _:Test.Failed => Result.Error
          case Test.Exhausted => Result.Skipped
          case _:Test.PropException => Result.Failure
        }
        val error = r.status match {
          case Test.PropException(_, e, _) => e
          case _ => null
        }
      }
    }

    def run(testClassName: String, fingerprint: TestFingerprint, args: Array[String]) = {

      def loadClass = {
        try {
          val obj = Class.forName(testClassName + "$", true, loader)
          obj.getField("MODULE$").get(null)
        } catch { case _ =>
          Class.forName(testClassName, true, loader).newInstance
        }
      }
      
      // TODO Loggers
      def propCallback(n: String, s: Int, d: Int) = {}

      def testCallback(n: String, r: Test.Result) = for(l <- loggers) {
        import Pretty._
        l.info(
          (if(r.passed) "+ " else "! ") + n + ": " + pretty(r, Params(0))
        )
      }

      // TODO val prms = Test.parseParams(args)
      val prms = Test.defaultParams

      fingerprint.superClassName match {
        case "org.scalacheck.Prop" => 
          val p = loadClass.asInstanceOf[Prop]
          Array(asEvent((testClassName, Test.check(prms, p))))
        case "org.scalacheck.Properties" =>
          val ps = loadClass.asInstanceOf[Properties]
          val rs = Test.checkProperties(ps, prms, propCallback, testCallback)
          rs.map(asEvent).toArray
      }
    }

  }

}
