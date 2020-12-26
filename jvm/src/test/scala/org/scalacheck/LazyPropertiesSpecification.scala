/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

object LazyPropertiesSpecification extends Properties("Properties.lazy registration") {

  property("properties registered lazily") = {
    var evaluated = false
    val p = new Properties("P") {
      property("p") = {
        evaluated = true
        Prop.proved
      }
    }
    evaluated == false
  }

}

