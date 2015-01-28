package org.scalacheck

import sbt.testing._

abstract class  BaseRunner(
                         val args: Array[String],
                         val remoteArgs: Array[String],
                         private[scalacheck] val loader: ClassLoader)
  extends Runner {

  protected def newTask(taskDef: TaskDef): Task =
    new ScalaCheckTask(taskDef, this)

  /** Called by test when it has finished executing */
  private[scalacheck] def testDone(statue: Status): Unit

  def serializeTask(task: Task, serializer: TaskDef => String): String =
    serializer(task.taskDef)

  def deserializeTask(task: String, deserializer: String => TaskDef): Task =
    newTask(deserializer(task))

  val results = new Results
}
