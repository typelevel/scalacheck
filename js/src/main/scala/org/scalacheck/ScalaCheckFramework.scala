/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2015 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import sbt.testing._
import org.scalajs.testinterface.TestUtils
import util.Pretty

private abstract class ScalaCheckRunner(
  val args: Array[String],
  val remoteArgs: Array[String],
  val loader: ClassLoader
) extends Runner {

  import java.util.concurrent.atomic.AtomicInteger

  def deserializeTask(task: String, deserializer: String => TaskDef) =
    ScalaCheckTask(deserializer(task), this)

  def serializeTask(task: Task, serializer: TaskDef => String) =
    serializer(task.taskDef)

  def tasks(taskDefs: Array[TaskDef]): Array[Task] =
    taskDefs.map(ScalaCheckTask(_, this))

  val successCount = new AtomicInteger(0)

  val failureCount = new AtomicInteger(0)

  val errorCount = new AtomicInteger(0)

  val testCount = new AtomicInteger(0)

  def testDone(status: Status) = {
    status match {
      case Status.Success => successCount.incrementAndGet()
      case Status.Error => errorCount.incrementAndGet()
      case Status.Skipped => errorCount.incrementAndGet()
      case Status.Failure => failureCount.incrementAndGet()
      case _ => failureCount.incrementAndGet()
    }
    testCount.incrementAndGet()
  }
}

private case class ScalaCheckTask(
  taskDef: TaskDef,
  runner: ScalaCheckRunner
) extends Task {

  val tags: Array[String] = Array()

  def execute(
    eventHandler: EventHandler,
    loggers: Array[Logger],
    continuation: Array[Task] => Unit
  ): Unit = continuation(execute(eventHandler, loggers))

  def execute(
    eventHandler: EventHandler,
    loggers: Array[Logger]
  ): Array[Task] = {

    def asEvent(n: String, r: Test.Result) = new Event {
      val status = r.status match {
        case Test.Passed => Status.Success
        case _:Test.Proved => Status.Success
        case _:Test.Failed => Status.Failure
        case Test.Exhausted => Status.Skipped
        case _:Test.PropException => Status.Error
      }
      val throwable = r.status match {
        case Test.PropException(_, e, _) => new OptionalThrowable(e)
        case _:Test.Failed => new OptionalThrowable(
          new Exception(Pretty.pretty(r,Pretty.Params(0)))
        )
        case _ => new OptionalThrowable()
      }
      val fullyQualifiedName = n
      val selector = new TestSelector(n)
      val fingerprint = taskDef.fingerprint
      val duration = -1L
    }

    val testCallback = new Test.TestCallback {
      override def onPropEval(n: String, w: Int, s: Int, d: Int) = {}
      override def onTestResult(n: String, r: Test.Result) = {
        import Pretty.{pretty, Params}
        val verbosityOpts = Set("-verbosity", "-v")
        val verbosity = runner.args
          .grouped(2).filter(twos => verbosityOpts(twos.head))
          .toSeq.headOption.map(_.last).map(_.toInt).getOrElse(0)
        val logMsg =
          (if (r.passed) "+ " else "! ") + s"$n: ${pretty(r, Params(verbosity))}"
        val ev = asEvent(n, r)
        eventHandler.handle(ev)
        loggers.foreach(l => l.info(logMsg))
        runner.testDone(ev.status)
      }
    }

    // TODO Hard-coded params! We should parse runner.args instead
    val prms = Test.Parameters.default
      .withMinSuccessfulTests(10)
      .withTestCallback(testCallback)
      .withCustomClassLoader(Some(runner.loader))

    import taskDef.{fingerprint, fullyQualifiedName}

    val fp = fingerprint.asInstanceOf[SubclassFingerprint]
    val obj =
      if (fp.isModule) TestUtils.loadModule(fullyQualifiedName, runner.loader)
      else TestUtils.newInstance(fullyQualifiedName, runner.loader)(Seq())

    if(obj.isInstanceOf[Properties])
      Test.checkProperties(prms, obj.asInstanceOf[Properties])
    else
      Test.check(prms, obj.asInstanceOf[Prop])

    Array()
  }

}

final class ScalaCheckFramework extends Framework {

  private def mkFP(mod: Boolean, cname: String, noArgCons: Boolean = true) =
    new SubclassFingerprint {
      def superclassName(): String = cname
      val isModule = mod
      def requireNoArgConstructor(): Boolean = noArgCons
    }

  val name = "ScalaCheck"

  def fingerprints(): Array[Fingerprint] = Array(
    mkFP(false, "org.scalacheck.Properties"),
    mkFP(false, "org.scalacheck.Prop"),
    mkFP(true, "org.scalacheck.Properties"),
    mkFP(true, "org.scalacheck.Prop")
  )

  def runner(args: Array[String], remoteArgs: Array[String],
    loader: ClassLoader
  ): Runner = new ScalaCheckRunner(args, remoteArgs, loader) {

    def receiveMessage(msg: String): Option[String] = msg(0) match {
      case 'd' =>
        val Array(t,s,f,e) = msg.tail.split(',')
        testCount.addAndGet(t.toInt)
        successCount.addAndGet(s.toInt)
        failureCount.addAndGet(f.toInt)
        errorCount.addAndGet(e.toInt)
        None
    }

    def done = {
      val heading = if (testCount == successCount) "Passed" else "Failed"
      s"$heading: Total $testCount, Failed $failureCount, Errors $errorCount, Passed $successCount"
    }

  }

  def slaveRunner(args: Array[String], remoteArgs: Array[String],
    loader: ClassLoader, send: String => Unit
  ): Runner = new ScalaCheckRunner(args, remoteArgs, loader) {

    def receiveMessage(msg: String) = None

    def done = {
      send(s"d$testCount,$successCount,$failureCount,$errorCount")
      ""
    }

  }

}
