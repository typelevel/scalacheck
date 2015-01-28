package org.scalacheck


import sbt.testing._
import java.util.concurrent.atomic.AtomicInteger

class  MasterRunner(
                    args: Array[String],
                    remoteArgs: Array[String],
                    loader: ClassLoader)
  extends BaseRunner(args, remoteArgs, loader) {

  def tasks(taskDefs: Array[TaskDef]): Array[Task] = {
    taskDefs.map(newTask)
  }

  def done(): String = {
    val test = results.testCount.get
    val success = results.successCount.get
    val failed = results.failureCount.get
    val errors = results.errorCount.get

    val heading = if (test == success) "Passed" else "Failed"
    s"$heading: Total $test, Failed $failed, Errors $errors, Passed $success"
  }

  private[scalacheck] def testDone(status: Status): Unit =  results.addTest(status)

  def receiveMessage(msg: String): Option[String] = msg(0) match {
    case 'd' =>
      // Slave notifies us of completion of tasks
      results.add(Results(msg.tail))
      None
  }
}