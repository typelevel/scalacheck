package org.scalacheck

import java.util.concurrent.atomic.AtomicInteger

import sbt.testing.Status

class Results {

  /** Number of successful tests on this node  */
  private[scalacheck] val successCount = new AtomicInteger(0)

  /** Number of failed tests on this node  */
  private[scalacheck] val failureCount = new AtomicInteger(0)

  /** Number of error tests on this node  */
  private[scalacheck] val errorCount = new AtomicInteger(0)

  /** Number of  tests on this node */
  private[scalacheck] val testCount = new AtomicInteger(0)

  private[scalacheck] def addTest(status: Status): Unit = {
    status match {
      case Status.Success => successCount.incrementAndGet()
      case Status.Error => errorCount.incrementAndGet()
      case Status.Skipped => errorCount.incrementAndGet()
      case Status.Failure => failureCount.incrementAndGet()
      case _ => failureCount.incrementAndGet()
    }
    testCount.incrementAndGet()
  }

  def set(test: Int, success: Int, failure: Int, error: Int):Results = {
    testCount.getAndSet(test)
    successCount.getAndSet(success)
    failureCount.getAndSet(failure)
    errorCount.getAndSet(error)
    this
  }

  override def toString:String = s"$testCount,$successCount,$failureCount,$errorCount"

  def add(c: Results):Unit = {
    testCount.addAndGet(c.testCount.get() )
    successCount.addAndGet(c.successCount.get() )
    failureCount.addAndGet(c.failureCount.get() )
    errorCount.addAndGet( c.errorCount.get())
  }
}

object Results {
  def apply(msg:String):Results = {
    val Array(t, s, f, e) = msg.split(',')
    val counter = new Results()
    counter.set(t.toInt, s.toInt, f.toInt, e.toInt)
  }

}
