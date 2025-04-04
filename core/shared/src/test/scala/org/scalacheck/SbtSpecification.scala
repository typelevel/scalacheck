/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package org.scalacheck

import sbt.testing.{
  Event,
  EventHandler,
  Framework,
  Selector,
  SuiteSelector,
  Task,
  TaskDef,
  TestSelector,
  TestWildcardSelector
}

object SbtFixture extends Properties("SbtFixture") {
  property("success") = Prop.passed
}

object SbtNestingFixture extends Properties("SbtNestingFixture") {
  include(SbtFixture)
}

object SbtSpecification extends Properties("Sbt") {
  property("suite") = run(Array(new SuiteSelector)) == List("SbtFixture.success")

  property("exact") = run(Array(new TestSelector("success"))) == List("SbtFixture.success")
  property("exactFull") = run(Array(new TestSelector("SbtFixture.success"))) == List("SbtFixture.success")
  property("exactMissing") = run(Array(new TestSelector("nonexistent"))) == List.empty

  property("wildcard") = run(Array(new TestWildcardSelector("succ"))) == List("SbtFixture.success")
  property("wildcardFull") = run(Array(new TestWildcardSelector("xture.succ"))) == List("SbtFixture.success")
  property("wildcardMissing") = run(Array(new TestWildcardSelector("prev"))) == List.empty

  property("nestedFull") = run(
    Array(new TestSelector("SbtNestingFixture.SbtFixture.success")),
    "org.scalacheck.SbtNestingFixture") == List("SbtNestingFixture.SbtFixture.success")

  property("nestedMedium") = run(
    Array(new TestSelector("SbtFixture.success")),
    "org.scalacheck.SbtNestingFixture") == List("SbtNestingFixture.SbtFixture.success")

  property("nestedShort") = run(
    Array(new TestSelector("success")),
    "org.scalacheck.SbtNestingFixture") == List("SbtNestingFixture.SbtFixture.success")

  // Since ScalaCheck does not keep track what class/object a property belongs to,
  // the following two issues concerning nested properties can not be fixed:
  //
  // When `explicitlySpecified = true`, properties from objects other than the one being run
  // should *not* run (and the outcome should be `List.empty`) - but they do.
  //
  // When a property from an object other than the one being run *does* run,
  // its status should be reported with a `NestedTestSelector`
  // where `suiteId` names the object that the property belongs to -
  // but it is reported with a `TestSelector`.

  property("nestedShouldRunAndDoes") = run(
    Array(new SuiteSelector),
    fullyQualifiedName = "org.scalacheck.SbtNestingFixture",
    explicitlySpecified = false
  ) == List("SbtNestingFixture.SbtFixture.success")

  property("nestedShouldNotRunButDoes") = run(
    Array(new SuiteSelector),
    fullyQualifiedName = "org.scalacheck.SbtNestingFixture",
    explicitlySpecified = true
  ) == List("SbtNestingFixture.SbtFixture.success") // should be List.empty

  // run using SBT Test Interface
  def run(
      selectors: Array[Selector],
      fullyQualifiedName: String = "org.scalacheck.SbtFixture",
      explicitlySpecified: Boolean = false
  ): List[String] = {
    val framework: Framework = new ScalaCheckFramework
    var ran: List[String] = List.empty
    val eventHandler: EventHandler =
      (event: Event) => ran = ran :+ event.selector().asInstanceOf[TestSelector].testName()
    def execute(tasks: Array[Task]): Unit = tasks.foreach(task => execute(task.execute(eventHandler, Array.empty)))
    execute(framework.runner(Array.empty, Array.empty, Platform.getClassLoader).tasks(Array(new TaskDef(
      fullyQualifiedName,
      framework.fingerprints()(2), // object ... extends org.scalacheck.Properties
      explicitlySpecified,
      selectors
    ))))
    ran
  }
}
