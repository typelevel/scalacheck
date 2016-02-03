/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2016 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck.util

import org.scalacheck.Properties

object PrettySpecification extends Properties("Pretty") {

  property("null") = Pretty.pretty(null) == "null"

  property("any null") = Pretty.pretty(null: Any) == "null"

}
