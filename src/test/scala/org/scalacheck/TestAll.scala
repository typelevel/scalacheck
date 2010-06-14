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
    val wrkSize = if(large) 200 else 20
    val workers = 
      if(args.contains("-4")) 4 
      else if(args.contains("-2")) 2 
      else 1


    val prms = 
      if(!large) Test.Params(workers = workers, wrkSize = wrkSize)
      else Test.Params(1000, 5000, 0, 10000, util.StdRand, workers, wrkSize)


    val propReport: (String,Int,Int) => Unit = 
      if(verbose) ConsoleReporter.propReport 
      else (n, i, j) => () 


    val testReport: (String,Test.Result) => Unit = 
      if(verbose) ConsoleReporter.testReport
      else (n, s) => s match {
        case r if r.passed => {}
        case _ => ConsoleReporter.testReport(n,s)
      }




    def measure[T](t: => T): (T,Long,Long) = {
      val start = System.currentTimeMillis
      val x = t
      val stop = System.currentTimeMillis
      (x,start,stop)
    }


    val (res,start,stop) = measure(
      Test.checkProperties(ScalaCheckSpecification, prms, propReport, testReport)
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
