package org.scalacheck


object PlatformShims {

  object TestShims {
    import Test._

    @inline def concurrentworkerFun(workers: Int, minSuccessfulTests: Int, maxDiscardRatio: Float, customClassLoader: Option[ClassLoader], zeroRes: Result, workerFun: Int => Result): Result = {

      def mergeResults(r1: Result, r2: Result): Result = {
        val Result(st1, s1, d1, fm1, _) = r1
        val Result(st2, s2, d2, fm2, _) = r2
        if (st1 != Passed && st1 != Exhausted)
          Result(st1, s1 + s2, d1 + d2, fm1 ++ fm2, 0)
        else if (st2 != Passed && st2 != Exhausted)
          Result(st2, s1 + s2, d1 + d2, fm1 ++ fm2, 0)
        else {
          if (s1 + s2 >= minSuccessfulTests && maxDiscardRatio * (s1 + s2) >= (d1 + d2))
            Result(Passed, s1 + s2, d1 + d2, fm1 ++ fm2, 0)
          else
            Result(Exhausted, s1 + s2, d1 + d2, fm1 ++ fm2, 0)
        }
      }

      import concurrent._
      val tp = java.util.concurrent.Executors.newFixedThreadPool(workers)
      implicit val ec = ExecutionContext.fromExecutor(tp)
      try {
        val fs = List.range(0, workers) map (idx => Future {
          customClassLoader.map(
            Thread.currentThread.setContextClassLoader(_)
          )
          blocking {
            workerFun(idx)
          }
        })
        //val zeroRes = Result(Passed,0,0,FreqMap.empty[Set[Any]],0)
        val res = Future.fold(fs)(zeroRes)(mergeResults)
        Await.result(res, concurrent.duration.Duration.Inf)
      } finally {
        tp.shutdown()
      }
    }
  }

  object TaskShims {
     @inline def parseParams(args: Array[String]): Option[Test.Parameters] = {
      import scala.language.reflectiveCalls
      import Test.cmdLineParser.{Success, NoSuccess}
      val prms = Test.cmdLineParser.parseParams(args) match {
        case Success(params, _) => params

        // TODO: Maybe handle this a bit better than throwing exception?
        case e: NoSuccess => throw new Exception(e.toString)
      }
      Some(prms)
    }
  }
}