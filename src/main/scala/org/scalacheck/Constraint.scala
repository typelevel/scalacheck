/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2009 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*-------------------------------------------------------------------------*/

package org.scalacheck

/** Contains predefined Constraint types */
object Constraint {

  implicit def unbox[T](ct: Constraint[T]): T = ct.unbox

  case class Pos[+T](val unbox: T) extends Constraint[T]

  case class Neg[+T](val unbox: T) extends Constraint[T]

  case class Alpha[+T](val unbox: T) extends Constraint[T]

  case class Numeric[+T](val unbox: T) extends Constraint[T]

  case class AlphaNum[+T](val unbox: T) extends Constraint[T]

  case class Small[+T](val unbox: T) extends Constraint[T]

  case class Large[+T](val unbox: T) extends Constraint[T]

}

/** A Constraint is an annotation for another type that tells
 *  ScalaCheck to generate only certain values of that type.
 *  The Constraint module contains a set of predefined constraints,
 *  and the Arbitrary module contains arbitrary generators a
 *  number of constraint/type pairs. Examples of constraints that
 *  can be used are Pos[Int], Alpha[String] etc.
 */
trait Constraint[+T] {
  
  def unbox: T

  override def toString = unbox.toString

  override def equals(x: Any) = unbox.equals(x)

}
