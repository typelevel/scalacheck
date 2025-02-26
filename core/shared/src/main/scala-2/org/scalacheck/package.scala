/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package org.scalacheck

object `package` {
  implicit class `by-name label :|`(private val prop: Prop) extends AnyVal {

    /** Label this property.
      *
      * The label is evaluated lazily. The operator name is chosen for its precedence btween boolean operators and
      * others.
      */
    def =|=(label: => String): Prop = prop.map(_.label(label))
  }
  // chained implicit for true =|= label
  implicit class `by-name label bool :| label`(private val bool: Boolean) extends AnyVal {
    def =|=(label: => String): Prop = (bool: Prop).=|=(label)
  }
  implicit class `by-name label |: prop`(label: => String) {
    def =|=(prop: Prop): Prop = prop.=|=(label)
  }
}
