/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

object NoPropertyNestingSpecification extends Properties("Properties.no nesting") {
  property("no nested properties") = {
    var thrown = false

    val p = new Properties("P") {
      property("outer") = {
        property("inner") = true // not allowed!
        true
      }
    }

    val results = for ((name, prop) <- p.properties) yield prop(Gen.Parameters.default)
    results match {
      case collection.Seq(res) => res.status match {
        case Prop.Exception(e: IllegalStateException) =>
          if (e.getMessage contains "nest") thrown = true
          else throw new Exception("exception message did not reference nesting")
        case _ => throw new Exception("did not get IllegalStateException")
      }
      case _ => throw new Exception("more than one property, somehow")
    }

    thrown == true
  }
}
