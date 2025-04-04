/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package org.scalacheck

import org.scalacheck.Test.Parameters
import org.scalacheck.Test.matchPropFilter
import sbt.testing.*

import java.util.concurrent.atomic.AtomicInteger

private abstract class ScalaCheckRunner extends Runner {

  val args: Array[String]
  val loader: ClassLoader
  val applyCmdParams: Parameters => Parameters

  val successCount = new AtomicInteger(0)
  val failureCount = new AtomicInteger(0)
  val errorCount = new AtomicInteger(0)
  val testCount = new AtomicInteger(0)

  def deserializeTask(task: String, deserializer: String => TaskDef): BaseTask = {
    val taskDef = deserializer(task)
    val countTestSelectors = taskDef.selectors().toSeq.count {
      case _: TestSelector => true
      case _ => false
    }
    if (countTestSelectors == 0) rootTask(taskDef)
    else checkPropTask(taskDef, single = true)
  }

  def serializeTask(task: Task, serializer: TaskDef => String) =
    serializer(task.taskDef())

  def tasks(taskDefs: Array[TaskDef]): Array[Task] = {
    val isForked = taskDefs.exists(_.fingerprint().getClass.getName.contains("ForkMain"))
    taskDefs.map(t => if (isForked) checkPropTask(t, single = false) else rootTask(t))
  }

  protected def sbtSetup(loader: ClassLoader): Parameters => Parameters =
    _.withTestCallback(new Test.TestCallback {}).withCustomClassLoader(Some(loader))

  abstract class BaseTask(_taskDef: TaskDef) extends Task {
    def taskDef() = _taskDef

    def tags(): Array[String] = Array.empty

    val loaded: Either[Prop, Properties] = {
      val fp = taskDef().fingerprint().asInstanceOf[SubclassFingerprint]
      val obj = if (fp.isModule()) Platform.loadModule(taskDef().fullyQualifiedName(), loader)
      else Platform.newInstance(taskDef().fullyQualifiedName(), loader, Seq())(Seq())
      obj match {
        case props: Properties => Right(props)
        case prop: Prop => Left(prop)
      }
    }

    val props: collection.Seq[(String, Prop)] = loaded match {
      case Right(ps) => ps.properties
      case Left(prop) => Seq("" -> prop)
    }

    val properties: Option[Properties] =
      loaded.toOption

    val params: Parameters = {
      // apply global parameters first, then allow properties to
      // override them. the other order does not work because
      // applyCmdParams will unconditionally set parameters to default
      // values, even when they were overridden.
      val ps = applyCmdParams(Parameters.default)
      properties.fold(ps)(_.overrideParameters(ps))
    }

    def log(loggers: Array[Logger], ok: Boolean, msg: String) =
      loggers foreach { l =>
        val logstr =
          if (!l.ansiCodesSupported()) msg
          else s"${if (ok) Console.GREEN else Console.RED}$msg${Console.RESET}"
        l.info(logstr)
      }

    def execute(handler: EventHandler, loggers: Array[Logger], continuation: Array[Task] => Unit): Unit =
      continuation(execute(handler, loggers))
  }

  def rootTask(td: TaskDef): BaseTask = new BaseTask(td) {
    def execute(handler: EventHandler, loggers: Array[Logger]): Array[Task] = {
      val isTestsOnly: Boolean = td.selectors().forall(selector =>
        selector.isInstanceOf[TestSelector] ||
          selector.isInstanceOf[TestWildcardSelector])

      def isIncluded(name: String): Boolean = !isTestsOnly || td.selectors().exists {
        case s: TestWildcardSelector => name.contains(s.testWildcard())
        // For TestSelector, exact comparison with both the full name and the name with
        // the suite prefix (properties.name) stripped off works for non-nested suite,
        // but results in a false negative for nested suites: test "A.B.test" can not be
        // selected by its short name "test";
        // instead, selected test name is matched using `endsWith`.
        case s: TestSelector => name.endsWith(s.testName())
        case _ => false
      }

      props.map(_._1).toSet.filter(isIncluded).toArray map { name =>
        checkPropTask(
          new TaskDef(
            td.fullyQualifiedName(),
            td.fingerprint(),
            td.explicitlySpecified(),
            Array(new TestSelector(name))),
          single = true)
      }
    }
  }

  def checkPropTask(taskDef: TaskDef, single: Boolean): BaseTask = new BaseTask(taskDef) { self =>
    def execute(handler: EventHandler, loggers: Array[Logger]): Array[Task] = {
      val propertyFilter = params.propFilter.map(_.r)

      if (single) {
        val mprops: Map[String, Prop] = props.toMap
        self.taskDef().selectors().foreach {
          case ts: TestSelector =>
            val name = ts.testName()
            mprops.get(name).foreach { prop =>
              executeInternal(prop, name, handler, loggers, propertyFilter)
            }
          case _ =>
            ()
        }
      } else {
        props.foreach { case (name, prop) =>
          executeInternal(prop, name, handler, loggers, propertyFilter)
        }
      }
      Array.empty[Task]
    }

    def executeInternal(
        prop: Prop,
        name: String,
        handler: EventHandler,
        loggers: Array[Logger],
        propertyFilter: Option[scala.util.matching.Regex]
    ): Unit = {
      if (propertyFilter.isEmpty || propertyFilter.exists(matchPropFilter(name, _))) {

        import util.Pretty.{pretty, Params}
        val result = Test.check(params, prop)

        val event = new Event {
          def status() = result.status match {
            case Test.Passed => Status.Success
            case _: Test.Proved => Status.Success
            case _: Test.Failed => Status.Failure
            case Test.Exhausted => Status.Failure
            case _: Test.PropException => Status.Error
          }
          def throwable() = result.status match {
            case Test.PropException(_, e, _) => new OptionalThrowable(e)
            case _: Test.Failed => new OptionalThrowable(
                new Exception(pretty(result, Params(0)))
              )
            case _ => new OptionalThrowable()
          }
          def fullyQualifiedName() = self.taskDef().fullyQualifiedName()
          def selector(): Selector = new TestSelector(name)
          def fingerprint() = self.taskDef().fingerprint()
          def duration() = -1L
        }

        handler.handle(event)

        event.status() match {
          case Status.Success => successCount.incrementAndGet()
          case Status.Error => errorCount.incrementAndGet()
          case Status.Skipped => errorCount.incrementAndGet()
          case Status.Failure => failureCount.incrementAndGet()
          case _ => failureCount.incrementAndGet()
        }
        testCount.incrementAndGet()

        // TODO Stack traces should be reported through event
        val verbosityOpts = Set("-verbosity", "-v")
        val verbosity =
          args.grouped(2).filter(twos => verbosityOpts(twos.head))
            .toSeq.headOption.map(_.last).map(_.toInt).getOrElse(0)
        val s = if (result.passed) "+" else "!"
        val n = if (name.isEmpty) self.taskDef().fullyQualifiedName() else name
        val logMsg = s"$s $n: ${pretty(result, Params(verbosity))}"
        log(loggers, result.passed, logMsg)
      }
    }
  }
}

final class ScalaCheckFramework extends Framework {

  private def mkFP(mod: Boolean, cname: String, noArgCons: Boolean = true) =
    new SubclassFingerprint {
      def superclassName(): String = cname
      def isModule() = mod
      def requireNoArgConstructor(): Boolean = noArgCons
    }

  override def name(): String = "ScalaCheck"

  override def fingerprints(): Array[Fingerprint] = Array(
    mkFP(false, "org.scalacheck.Properties"),
    mkFP(false, "org.scalacheck.Prop"),
    mkFP(true, "org.scalacheck.Properties"),
    mkFP(true, "org.scalacheck.Prop")
  )

  override def runner(_args: Array[String], _remoteArgs: Array[String], _loader: ClassLoader): Runner =
    new ScalaCheckRunner {

      val args = _args
      def remoteArgs() = _remoteArgs
      val loader = _loader
      val (prms, unknownArgs) = Test.CmdLineParser.parseParams(args)
      val applyCmdParams = prms.andThen(sbtSetup(loader))

      def receiveMessage(msg: String): Option[String] = msg(0) match {
        case 'd' =>
          val Array(t, s, f, e) = msg.tail.split(',')
          testCount.addAndGet(t.toInt)
          successCount.addAndGet(s.toInt)
          failureCount.addAndGet(f.toInt)
          errorCount.addAndGet(e.toInt)
          None
      }

      override def done() = if (testCount.get > 0) {
        val heading = if (testCount.get == successCount.get) "Passed" else "Failed"
        s"$heading: Total $testCount, " +
          s"Failed $failureCount, Errors $errorCount, Passed $successCount" +
          (if (unknownArgs.isEmpty) ""
           else
             s"\nWarning: Unknown ScalaCheck args provided: ${unknownArgs.mkString(" ")}")
      } else ""

    }

  def slaveRunner(
      _args: Array[String],
      _remoteArgs: Array[String],
      _loader: ClassLoader,
      send: String => Unit
  ): _root_.sbt.testing.Runner = new ScalaCheckRunner {
    val args = _args
    def remoteArgs() = _remoteArgs
    val loader = _loader

    val (prms, unknownArgs) = Test.CmdLineParser.parseParams(args)

    if (unknownArgs.nonEmpty) {
      println(s"Warning: Unknown ScalaCheck args provided: ${unknownArgs.mkString(" ")}")
    }

    val applyCmdParams = prms.andThen(sbtSetup(loader))

    def receiveMessage(msg: String): Option[String] = None

    override def done() = {
      send(s"d$testCount,$successCount,$failureCount,$errorCount")
      ""
    }
  }
}
