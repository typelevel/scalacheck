package org.scalacheck

object Util {

  sealed trait Either[+T,+U]
  case class Left[+T,+U](x: T) extends Either[T,U]
  case class Right[+T,+U](x: U) extends Either[T,U]

  def secure[T](x: => T): Either[T,Throwable] =
    try { Left(x) } catch { case e => Right(e) }

}

