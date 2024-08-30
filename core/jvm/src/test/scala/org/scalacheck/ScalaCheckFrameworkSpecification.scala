/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package org.scalacheck

import org.scalacheck.Prop.{all, proved}
import sbt.testing.{Selector, SuiteSelector, TaskDef, TestSelector}

object ScalaCheckFrameworkSpecification extends Properties("ScalaCheckFramework") {

  private val firstProp = "ScalaCheckFrameworkHelper.first prop"
  private val secondProp = "ScalaCheckFrameworkHelper.second prop"
  private val thirdProp = "ScalaCheckFrameworkHelper.third prop"

  property("all props with SuiteSelector") = all(
    getPropNamesForSelectors(List(new SuiteSelector)) == List(firstProp, secondProp, thirdProp),
    getPropNamesForSelectors(List(new SuiteSelector, new TestSelector(firstProp))) == List(
      firstProp,
      secondProp,
      thirdProp),
    getPropNamesForSelectors(List(new SuiteSelector, new TestSelector("no matches"))) == List(
      firstProp,
      secondProp,
      thirdProp)
  )

  property("only matching props with TestSelector") = all(
    getPropNamesForSelectors(List(new TestSelector(firstProp))) == List(firstProp),
    getPropNamesForSelectors(List(new TestSelector(secondProp))) == List(secondProp),
    getPropNamesForSelectors(List(new TestSelector(firstProp), new TestSelector(thirdProp))) == List(
      firstProp,
      thirdProp),
    getPropNamesForSelectors(List(new TestSelector("no matches"))) == Nil
  )

  private def getPropNamesForSelectors(selectors: List[Selector]): List[String] = {
    val framework = new ScalaCheckFramework()
    val runner = framework.runner(Array.empty, Array.empty, getClass.getClassLoader).asInstanceOf[ScalaCheckRunner]
    val taskDef = new TaskDef(
      classOf[ScalaCheckFrameworkSpecificationHelper].getName,
      framework.fingerprints()(0),
      true,
      selectors.toArray)
    val baseTask = runner.rootTask(taskDef)
    val newTasks = baseTask.execute(null, null)
    val propNames = for {
      task <- newTasks
      selector <- task.taskDef().selectors()
    } yield selector.asInstanceOf[TestSelector].testName()
    propNames.toList
  }
}

class ScalaCheckFrameworkSpecificationHelper extends Properties("ScalaCheckFrameworkHelper") {
  property("first prop") = proved
  property("second prop") = proved
  property("third prop") = proved
}
