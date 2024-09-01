/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package org.scalacheck

import scala.annotation.nowarn
import scala.scalanative.reflect.Reflect

import Test.*

private[scalacheck] object Platform {

  @nowarn("msg=is never used")
  def runWorkers(
      params: Parameters,
      workerFun: Int => Result,
      stop: () => Unit
  ): Result = {
    workerFun(0)
  }

  @nowarn("msg=is never used")
  def loadModule(name: String, loader: ClassLoader): AnyRef = {
    Reflect
      .lookupLoadableModuleClass(name + "$")
      .getOrElse(throw new ClassNotFoundException(name + "$"))
      .loadModule()
      .asInstanceOf[AnyRef]
  }

  @nowarn("msg=is never used")
  def newInstance(name: String, loader: ClassLoader, paramTypes: Seq[Class[?]])(args: Seq[AnyRef]): AnyRef = {
    Reflect
      .lookupInstantiatableClass(name)
      .getOrElse(throw new ClassNotFoundException(name))
      .getConstructor(paramTypes*)
      .getOrElse(throw new NoSuchMethodError(paramTypes.mkString("<init>(", ",", ")")))
      .newInstance(args*)
      .asInstanceOf[AnyRef]
  }

  type EnableReflectiveInstantiation = scala.scalanative.reflect.annotation.EnableReflectiveInstantiation
}
