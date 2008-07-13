/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2008 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*-------------------------------------------------------------------------*/

package org.scalacheck

object ConsoleReporter {

  def prettyTestRes(res: Test.Result) = (res.status match {
    case Test.Proved(args) =>
      "OK, proved property" +
      (if(args.isEmpty) "\n" else ":                   \n" + prettyArgs(args))
    case Test.Passed =>
      "OK, passed " + res.succeeded + " tests."
    case Test.Failed(args) =>
      "Falsified after "+res.succeeded+" passed tests" +
      (if(args.isEmpty) "\n" else ":      \n" + prettyArgs(args))
    case Test.Exhausted =>
      "Gave up after only " + res.succeeded + " passed tests. " +
      res.discarded + " tests were discarded."
    case Test.PropException(args,e) =>
      "Exception \"" + e + "\" raised on property evaluation" +
      (if(args.isEmpty) "\n" else ":\n" + prettyArgs(args))
    case Test.GenException(e) =>
      "Exception \"" + e + "\" raised on argument generation."
  }) + prettyFreqMap(res.freqMap)

  def prettyArgs(args: List[Arg]) = {
    val strs = for((a,i) <- args.zipWithIndex) yield (
      "> " +
      (if(a.label == "") "ARG_" + i else a.label) + 
      " = \"" + a.arg + 
      (if(a.shrinks > 0) "\" (" + a.shrinks + " shrinks)" else "\"")
    )
    strs.mkString("\n")
  }

  def prettyFreqMap(fm: FreqMap[Any]) = if(fm.total == 0) "" else {
    val rs = fm.getRatios.map { case (x,r) => x.toString + ":\t" + (r*100).toString + " %" }
    rs.mkString("\n","\n","\n")
  }

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
    val s = prettyTestRes(testRes)
    printf("%s %s%s\n", if(testRes.passed) "+" else "!", s, 
      List.make(70 - s.length, " ").mkString(""))
    testRes
  }

  def testReport(pName: String, res: Test.Result) =
  {
    def printL(t: String, label: String, str: String) =
      printf("%s %s: %s%s\n", t, label, str,
        List.make(70 - str.length - label.length, " ").mkString(""))

    printL(if(res.passed) "+" else "!", pName, prettyTestRes(res))
  }
  
  def testStatsEx(res: Test.Result): Unit = testStatsEx("", res)
  
  def testStatsEx(msg: String, res: Test.Result) = {
    lazy val m = if(msg.length == 0) "" else msg + ": "
    res.status match {
      case Test.Proved(_) => {}
      case Test.Passed => {}
      case f @ Test.Failed(_) => error(m + f)
      case Test.Exhausted => {}
      case f @ Test.GenException(_) => error(m + f)
      case f @ Test.PropException(_, _) => error(m + f)
    }
  }

}
