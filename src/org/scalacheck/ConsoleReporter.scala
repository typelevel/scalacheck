/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2008 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*-------------------------------------------------------------------------*/

package org.scalacheck

import Pretty._
import util.FreqMap

object ConsoleReporter {

  def propReport(s: Int, d: Int) =
  {
    if(d == 0) printf("\rPassed %s tests\r", s)
    else printf("\rPassed %s tests; %s discarded\r", s, d)
    Console.flush
  }

  def propReport(pName: String, s: Int, d: Int) =
  {
    if(d == 0) printf("\r  %s: Passed %s tests\r", pName, s)
    else printf("\r  %s: Passed %s tests; %s discarded\r", pName, s, d)
    Console.flush
  }

  def testReport(testRes: Test.Result) =
  {
    val s = pretty(testRes)
    printf("%s %s%s\n", if(testRes.passed) "+" else "!", s,
      List.make(70 - s.length, " ").mkString(""))
    testRes
  }

  def testReport(pName: String, res: Test.Result) =
  {
    def printL(t: String, label: String, str: String) =
      printf("%s %s: %s%s\n", t, label, str,
        List.make(70 - str.length - label.length, " ").mkString(""))

    printL(if(res.passed) "+" else "!", pName, pretty(res))
  }

  def testStatsEx(res: Test.Result): Unit = testStatsEx("", res)

  def testStatsEx(msg: String, res: Test.Result) = {
    lazy val m = if(msg.length == 0) "" else msg + ": "
    res.status match {
      case Test.Proved(_) => {}
      case Test.Passed => {}
      case f @ Test.Failed(_, _) => error(m + f)
      case Test.Exhausted => {}
      case f @ Test.GenException(_) => error(m + f)
      case f @ Test.PropException(_, _, _) => error(m + f)
    }
  }

}
