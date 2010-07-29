package org.scalacheck

object TestAll {
  object ScalaCheckSpecification extends Properties("ScalaCheck") {
    include(GenSpecification)
    include(PropSpecification)
    include(TestSpecification)
  }

  def main(args: Array[String]) {
    val verbose = args.contains("-v")
    val large = args.contains("-l")
    val workers = 
      if(args.contains("-4")) 4 
      else if(args.contains("-2")) 2 
      else 1

    val testCallback = new Test.TestCallback {
      override def onPropEval(n:String,w:Int,s:Int,d:Int) =
        if(verbose) ConsoleReporter(0).onPropEval(n,w,s,d)

      override def onTestResult(n:String,r:Test.Result) =
        if(verbose || !r.passed) ConsoleReporter(0).onTestResult(n,r)
    }

    val prms = {
      val p = 
        if(!large) Test.Params(workers = workers)
        else Test.Params(1000, 5000, 0, 10000, util.StdRand, workers)
      p copy (testCallback = testCallback)
    }

    def measure[T](t: => T): (T,Long,Long) = {
      val start = System.currentTimeMillis
      val x = t
      val stop = System.currentTimeMillis
      (x,start,stop)
    }

    val (res,start,stop) = measure(
      Test.checkProperties(prms, ScalaCheckSpecification)
    )

    val min = (stop-start)/(60*1000)
    val sec = ((stop-start)-(60*1000*min)) / 1000d

    val passed = res.filter(_._2.passed).size
    val failed = res.filter(!_._2.passed).size

    if(verbose || failed > 0) println

    if(passed > 0) printf("%s test%s PASSED\n", passed, if(passed != 1) "s" else "")
    if(failed > 0) printf("%s test%s FAILED\n", failed, if(failed != 1) "s" else "")
    printf("Elapsed time: %s min %s sec\n", min, sec)

    exit(failed)
  }
}
