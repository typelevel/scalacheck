package org.scalacheck

/** Contains predefined Constraint types */
object Constraint {

  implicit def unwrap[T](ct: Constraint[T]): T = ct.wrapped

  case class Pos[+T](val wrapped: T) extends Constraint[T]

  case class Neg[+T](val wrapped: T) extends Constraint[T]

  case class Alpha[+T](val wrapped: T) extends Constraint[T]

  case class Numeric[+T](val wrapped: T) extends Constraint[T]

  case class AlphaNum[+T](val wrapped: T) extends Constraint[T]

  case class Small[+T](val wrapped: T) extends Constraint[T]

  case class Large[+T](val wrapped: T) extends Constraint[T]

}

/** A Constraint is an annotation for another type that tells
 *  ScalaCheck to generate only certain values of that type.
 *  The Constraint module contains a set of predefined constraints,
 *  and the Arbitrary module contains arbitrary generators a
 *  number of constraint/type pairs. Examples of constraints that
 *  can be used are Pos[Int], Alpha[String] etc.
 */
trait Constraint[+T] {
  
  def wrapped: T

  override def toString = wrapped.toString

}
