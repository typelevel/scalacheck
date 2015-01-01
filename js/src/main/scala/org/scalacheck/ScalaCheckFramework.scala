/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2015 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

object ScalaCheckFramework extends scalajs.test.TestFramework {

  import scalajs.test.{Test => TestJS}
  import scalajs.test.TestOutput
  import scalajs.js.{Array, Function0}

  // TODO Complete the parser, and move it to the shared src, replacing
  // CmdLineParser (to avoid using parser combinators entirely)
  private def parseParams(args: Array[String]): Option[Test.Parameters] = {
    var params = Test.Parameters.default.withMinSuccessfulTests(10)
    Some(params)
  }

  def runTest(out: TestOutput, args: Array[String])(tf: Function0[TestJS]) = {

    val testCallback = new Test.TestCallback {
      override def onPropEval(n: String, w: Int, s: Int, d: Int) = {}

      override def onTestResult(n: String, r: Test.Result) = {
        import util.Pretty._
        val verbosityOpts = Set("-verbosity", "-v")
        val verbosity = args.grouped(2).filter(twos => verbosityOpts(twos.head)).toSeq.headOption.map(_.last).map(_.toInt).getOrElse(0)
        val msg = s"$n: ${pretty(r, Params(verbosity))}"
        if(r.passed) out.succeeded(s"+ $msg")
        else out.failure(s"! $msg")
      }
    }

    parseParams(args) match {
      case None => out.error("Unable to parse ScalaCheck arguments: $args")
      case Some(params) =>
        val prms = params.withTestCallback(testCallback)
        val obj = tf()
        if(obj.isInstanceOf[Properties])
          Test.checkProperties(prms, obj.asInstanceOf[Properties])
        else if(obj.isInstanceOf[Prop])
          Test.check(prms, obj.asInstanceOf[Prop])
    }

  }

}
