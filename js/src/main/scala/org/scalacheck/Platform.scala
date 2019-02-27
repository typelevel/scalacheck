/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2018 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import scala.scalajs.reflect.Reflect

import Test._

private[scalacheck] object Platform {

  def runWorkers(
    params: Parameters,
    workerFun: Int => Result,
    stop: () => Unit
  ): Result = {
    workerFun(0)
  }

  def loadModule(name: String, loader: ClassLoader): AnyRef = {
    Reflect
      .lookupLoadableModuleClass(name + "$")
      .getOrElse(throw new ClassNotFoundException(name + "$"))
      .loadModule()
      .asInstanceOf[AnyRef]
  }

  def newInstance(name: String, loader: ClassLoader, paramTypes: Seq[Class[_]])(args: Seq[AnyRef]): AnyRef = {
    Reflect
      .lookupInstantiatableClass(name)
      .getOrElse(throw new ClassNotFoundException(name))
      .getConstructor(paramTypes: _*)
      .getOrElse(throw new NoSuchMethodError(paramTypes.mkString("<init>(", ",", ")")))
      .newInstance(args: _*)
      .asInstanceOf[AnyRef]
  }

  type EnableReflectiveInstantiation = scala.scalajs.reflect.annotation.EnableReflectiveInstantiation
}
