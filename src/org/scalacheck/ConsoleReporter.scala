/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2008 Rickard Nilsson. All rights reserved.          **
**  http://code.google.com/p/scalacheck/                                   **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*-------------------------------------------------------------------------*/

package org.scalacheck

object ConsoleReporter {

  def prettyTestStats(stats: Test.Stats) = stats.result match {
    case Test.Proved(args) =>
      "OK, proved property:                   \n" + prettyArgs(args)
    case Test.Passed =>
      "OK, passed " + stats.succeeded + " tests."
    case Test.Failed(args) =>
      "Falsified after "+stats.succeeded+" passed tests:\n"+prettyArgs(args)
    case Test.Exhausted =>
      "Gave up after only " + stats.succeeded + " passed tests. " +
      stats.discarded + " tests were discarded."
    case Test.PropException(args,e) =>
      "Exception \"" + e + "\" raised on property evaluation:\n" +
      prettyArgs(args)
    case Test.GenException(e) =>
      "Exception \"" + e + "\" raised on argument generation."
  }

  def prettyArgs(args: List[Arg]) = {
    val strs = for((a,i) <- args.zipWithIndex) yield (
      "> " +
      (if(a.label == "") "ARG_" + i else a.label) + 
      " = \"" + a.arg + 
      (if(a.shrinks > 0) "\" (" + a.shrinks + " shrinks)" else "\"")
    )
    strs.mkString("\n")
  }

  def propReport(s: Int, d: Int) =
  {
    if(d == 0) printf("\rPassed %s tests\r", s)
    else printf("\rPassed %s tests; %s discarded\r", s, d)
    Console.flush
  }

  def propReport(pName: String, s: Int, d: Int) =
  {
    if(d == 0) printf("\r  %s: Passed %s tests\r", s, pName)
    else printf("\r  %s: Passed %s tests; %s discarded\r", pName, s, d)
    Console.flush
  }

  def testReport(testStats: Test.Stats) =
  {
    val s = prettyTestStats(testStats)
    printf("%s %s%s\n", if(testStats.result.passed) "+" else "!", s, 
      List.make(70 - s.length, " ").mkString(""))
    testStats
  }

  def testReport(pName: String, stats: Test.Stats) =
  {
    def printL(t: String, label: String, str: String) =
      printf("%s %s: %s%s\n", t, label, str,
        List.make(70 - str.length - label.length, " ").mkString(""))

    printL(if(stats.result.passed) "+" else "!", pName, prettyTestStats(stats))
  }
  
  def testStatsEx(stats: Test.Stats): Unit = testStatsEx("", stats)
  
  def testStatsEx(msg: String, stats: Test.Stats) = {
    lazy val m = if(msg.length == 0) "" else msg + ": "
    stats.result match {
      case Test.Proved(_) => {}
      case Test.Passed => {}
      case f @ Test.Failed(_) => error(m + f)
      case Test.Exhausted => {}
      case f @ Test.GenException(_) => error(m + f)
      case f @ Test.PropException(_, _) => error(m + f)
    }
  }

}
