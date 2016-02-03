/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2016 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import Test._

private[scalacheck] object Platform {

  import util.FreqMap

  def runWorkers(
    params: Parameters,
    workerFun: Int => Result,
    stop: () => Unit
  ): Result = {
    import params._

    def mergeResults(r1: Result, r2: Result): Result = {
      val Result(st1, s1, d1, fm1, _) = r1
      val Result(st2, s2, d2, fm2, _) = r2
      if (st1 != Passed && st1 != Exhausted)
        Result(st1, s1+s2, d1+d2, fm1++fm2, 0)
      else if (st2 != Passed && st2 != Exhausted)
        Result(st2, s1+s2, d1+d2, fm1++fm2, 0)
      else {
        if (s1+s2 >= minSuccessfulTests && maxDiscardRatio*(s1+s2) >= (d1+d2))
          Result(Passed, s1+s2, d1+d2, fm1++fm2, 0)
        else
          Result(Exhausted, s1+s2, d1+d2, fm1++fm2, 0)
      }
    }

    if(workers < 2) workerFun(0)
    else {
      import concurrent._
      val tp = java.util.concurrent.Executors.newFixedThreadPool(workers)
      implicit val ec = ExecutionContext.fromExecutor(tp)
      try {
        val fs = List.range(0,workers) map (idx => Future {
          params.customClassLoader.map(
            Thread.currentThread.setContextClassLoader(_)
          )
          blocking { workerFun(idx) }
        })
        val zeroRes = Result(Passed,0,0,FreqMap.empty[Set[Any]],0)
        val res = Future.fold(fs)(zeroRes)(mergeResults)
        Await.result(res, concurrent.duration.Duration.Inf)
      } finally {
        stop()
        tp.shutdown()
      }
    }
  }

  def newInstance(name: String, loader: ClassLoader)(args: Seq[AnyRef]): AnyRef =
    if(!args.isEmpty) ???
    else Class.forName(name, true, loader).newInstance.asInstanceOf[AnyRef]

  def loadModule(name: String, loader: ClassLoader): AnyRef =
    Class.forName(name + "$", true, loader).getField("MODULE$").get(null)

  import scala.annotation.Annotation

  class JSExportDescendentObjects(ignoreInvalidDescendants: Boolean)
      extends scala.annotation.Annotation {
    def this() = this(false)
  }

  class JSExportDescendentClasses(ignoreInvalidDescendants: Boolean)
      extends scala.annotation.Annotation {
    def this() = this(false)
  }
}
