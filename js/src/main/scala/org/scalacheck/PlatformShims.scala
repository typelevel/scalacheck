package org.scalacheck

import sbt.testing._
import Prop.Arg

object PlatformShims {

  object TestShims {
    import org.scalacheck.Test._
    @inline def concurrentworkerFun(workers: Int, minSuccessfulTests: Int, maxDiscardRatio: Float, customClassLoader: Option[ClassLoader], zeroRes: Result, workerFun: Int => Result): Result = {
      workerFun(0)
    }
  }

  object TaskShims {
    // TODO Complete the parser, and move it to the shared src, replacing
    // CmdLineParser (to avoid using parser combinators entirely)
    @inline def parseParams(args: Array[String]): Option[Test.Parameters] = {
      val params = Test.Parameters.default.withMinSuccessfulTests(10).withWorkers(1)
      Some(params)
    }
  }

}
