package org.scalacheck

import sbt.testing._

final class SlaveRunner(
                         args: Array[String],
                         remoteArgs: Array[String],
                         testClassLoader: ClassLoader,
                         send: String => Unit
                         ) extends BaseRunner(args, remoteArgs, testClassLoader) {

  def tasks(taskDefs: Array[TaskDef]): Array[Task] = {
    // Notify master of new tasks
    send("t" + taskDefs.length)
    taskDefs.map(newTask)
  }

  def done(): String = {
    send("d" + results)
    "" // <- ignored
  }

  private[scalacheck] def testDone(status: Status): Unit =  results.addTest(status)

  def receiveMessage(msg: String): Option[String] = {
    None // <- ignored
  }

  override def serializeTask(task: Task,
                             serializer: TaskDef => String): String = {
    super.serializeTask(task, serializer)
  }

  override def deserializeTask(task: String,
                               deserializer: String => TaskDef): Task = {
    super.deserializeTask(task, deserializer)
  }
}