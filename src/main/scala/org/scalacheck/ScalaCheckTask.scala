package org.scalacheck


import sbt.testing._
import org.scalajs.testinterface.TestUtils
import org.scalacheck.util.Pretty
import scala.util.Try


class ScalaCheckTask(task: TaskDef, runner: BaseRunner) extends Task {
  val fingerprint = taskDef().fingerprint()
  val testClassName = taskDef().fullyQualifiedName()

  def tags(): Array[String] = Array.ofDim[String](0)

  def taskDef(): TaskDef = task

  def execute(eventHandler: EventHandler, loggers: Array[Logger],
              continuation: (Array[Task]) => Unit): Unit = {
    continuation(execute(
      eventHandler, loggers))
  }

  private def asEvent(nr: (String, Test.Result), taskDef: TaskDef): Event = nr match {

    case (n: String, r: Test.Result) => new Event {
      val testName = n
      val description = n

      val result = r.status match {
        case Test.Passed   => Status.Success
        case _: Test.Proved => Status.Success
        case _: Test.Failed => Status.Failure
        case Test.Exhausted => Status.Skipped
        case _: Test.PropException => Status.Error
      }
      val error = r.status match {
        case Test.PropException(_, e, _) => e
        case _: Test.Failed => new Exception(Pretty.pretty(r, Pretty.Params(0)))
        case _ => null
      }

      override def fullyQualifiedName(): String = n //testClassName

      override def throwable(): OptionalThrowable = if(error == null) new OptionalThrowable() else new OptionalThrowable(error)

      override def status(): Status = result

      override def selector(): Selector = new TestSelector(n) //testClassName)

      override def fingerprint(): Fingerprint = taskDef.fingerprint

      override def duration(): Long = 0
    }
  }

  def loadClass(name: String, loader: ClassLoader): Prop = {
    Try(TestUtils.newInstance(name, loader)(Seq())).toOption
      .collect { case ref: Prop => ref}.getOrElse(throw new Exception(s"Unable to load class $name"))
  }

  def loadModule(name: String, loader: ClassLoader): Prop = {
    Try(TestUtils.loadModule(name, loader)).toOption
      .collect { case ref: Prop => ref}.getOrElse(throw new Exception(s"Unable to load module $name"))
  }

  def execute(handler: EventHandler, loggers: Array[Logger]): Array[Task] = {

    val testCallback = new Test.TestCallback {
      override def onPropEval(n: String, w: Int, s: Int, d: Int): Unit = {}

      override def onTestResult(n: String, r: Test.Result): Unit = {
        import Pretty._
        val verbosityOpts = Set("-verbosity", "-v")
        val verbosity = runner.args.grouped(2).filter(twos => verbosityOpts(twos.head)).toSeq.headOption.map(_.last).map(_.toInt).getOrElse(0)
        val logMsg = (if (r.passed) "+ " else "! ") + s"$n: ${pretty(r, Params(verbosity))}"
        val ev = asEvent((n, r), taskDef())

        handler.handle(ev)
        loggers.foreach(l => l.info(logMsg))
        runner.testDone(ev.status())
      }
    }

    import PlatformShims.TaskShims._
    val prms = parseParams(runner.args) match {
      case None => throw new Exception("Unable to parse ScalaCheck arguments: $args")
      case Some(params) =>
        params.withTestCallback(testCallback).withCustomClassLoader(Some(runner.loader))
    }

    fingerprint match {
      case fp: SubclassFingerprint =>
        val obj = if (fp.isModule()) loadModule(testClassName, runner.loader)
                  else loadClass(testClassName, runner.loader)
        obj match {
          case p: Properties => Test.checkProperties(prms, p)
          case p: Prop => Test.check(prms, p)
          case _ => throw new Exception(s"Unknown test instance $obj")
        }
    }

    Array.empty
  }
}
