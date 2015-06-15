/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2015 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import Test._

private[scalacheck] object Platform {

  def runWorkers(
    params: Parameters,
    workerFun: Int => Result,
    stop: () => Unit
  ): Result = {
    workerFun(0)
  }

  def loadModule(name: String, loader: ClassLoader): AnyRef =
    org.scalajs.testinterface.TestUtils.loadModule(name, loader)

  def newInstance(name: String, loader: ClassLoader)(args: Seq[AnyRef]): AnyRef =
    org.scalajs.testinterface.TestUtils.newInstance(name, loader)(args)

  type JSExportDescendentObjects = scala.scalajs.js.annotation.JSExportDescendentObjects

  type JSExportDescendentClasses = scala.scalajs.js.annotation.JSExportDescendentClasses
}
