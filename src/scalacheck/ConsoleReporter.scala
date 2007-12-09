package scalacheck

object ConsoleReporter {

  def prettyTestStats(stats: Test.Stats) = stats.result match {
    case Test.Passed() =>
      "OK, passed " + stats.succeeded + " tests."
    case Test.Failed(args) =>
      "Falsified after "+stats.succeeded+" passed tests:\n"+prettyArgs(args)
    case Test.Exhausted() =>
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
      " = " + a.arg + 
      (if(a.shrinks > 0) " (" + a.shrinks + " shrinks)" else "")
    )
    strs.mkString("\n")
  }

  def propReport(res: Option[Prop.Result], s: Int, d: Int) =
  {
    if(d == 0) printf("\rPassed {0} tests", s)
    else printf("\rPassed {0} tests; {1} discarded", s, d)
    Console.flush
  }

  def propReport(pName: String, res: Option[Prop.Result], s: Int, d: Int) =
  {
    if(d == 0) printf("\r  {1}: Passed {0} tests", s, pName)
    else printf("\r  {2}: Passed {0} tests; {1} discarded", s, d, pName)
    Console.flush
  }

  def testReport(testStats: Test.Stats) =
  {
    val s = prettyTestStats(testStats)
    printf("\r{2} {0}{1}\n", s, List.make(70 - s.length, " ").mkString(""),
      if(testStats.result.passed) "+" else "!")
    testStats
  }

  def testReport(pName: String, stats: Test.Stats) =
  {
    def printL(t: String, label: String, str: String) =
      printf("\r{0} {1}: {2}{3}\n", t, label, str,
        List.make(70 - str.length - label.length, " ").mkString(""))

    printL(if(stats.result.passed) "+" else "!", pName, prettyTestStats(stats))
  }
  
  def testStatsEx(stats: Test.Stats): Unit = testStatsEx("", stats)
  
  def testStatsEx(msg: String, stats: Test.Stats) = {
    lazy val m = if(msg.isEmpty) "" else msg + ": "
    stats.result match {
      case Test.Passed() => {}
      case f @ Test.Failed(_) => error(m + f)
      case Test.Exhausted() => {}
      case f @ Test.GenException(_) => error(m + f)
      case f @ Test.PropException(_, _) => error(m + f)
    }
  }

}
