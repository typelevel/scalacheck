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

class ScalaCheckFramework extends Framework {

  private def mkFP(mod: Boolean, cname: String, noArgCons: Boolean = true) =
    new SubclassFingerprint {
      def superclassName(): String = cname
      val isModule = mod
      def requireNoArgConstructor(): Boolean = noArgCons
    }

  val name = "ScalaCheck"

  def fingerprints(): Array[Fingerprint] = {
    Array(
      mkFP(false, "org.scalacheck.Properties"),
      mkFP(false, "org.scalacheck.Prop"),
      mkFP(true, "org.scalacheck.Properties"),
      mkFP(true, "org.scalacheck.Prop")

    )
  }

  def runner(args: Array[String], remoteArgs: Array[String], loader: ClassLoader): Runner = {
    new MasterRunner(args, remoteArgs, loader)
  }

  def slaveRunner(args: Array[String], remoteArgs: Array[String], loader: ClassLoader, send: (String) => Unit): Runner =
    new SlaveRunner(args, remoteArgs, loader, send)
}
