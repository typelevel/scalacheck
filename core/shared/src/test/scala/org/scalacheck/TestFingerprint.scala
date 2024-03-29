/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package org.scalacheck

class PropsClass extends Properties("TestFingerprint") {
  property("propclass") = Prop.proved
}

object PropsObject extends Properties("TestFingerprint") {
  property("propobject") = Prop.proved
}
