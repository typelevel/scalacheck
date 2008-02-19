/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2008 Rickard Nilsson. All rights reserved.          **
**  http://code.google.com/p/scalacheck/                                   **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*-------------------------------------------------------------------------*/

package org.scalacheck

sealed trait Shrink[T] {
  def shrink(x: T): Stream[T]
}

object Shrink {

  import Stream.{cons, empty}

  def apply[T](s: T => Stream[T]): Shrink[T] = new Shrink[T] {
    override def shrink(x: T) = s(x)
  }

  def shrink[T](x: T)(implicit s: Shrink[T]): Stream[T] = s.shrink(x)

  /** Default shrink instance */
  implicit def shrinkAny[T]: Shrink[T] = Shrink(x => empty)

  /** Shrink instance of integer */
  implicit lazy val shrinkInt: Shrink[Int] = Shrink { n =>

    def iterate[T](f: T => T, x: T): Stream[T] = {
      val y = f(x)
      cons(y, iterate(f,y))
    }

    if(n == 0) empty
    else {
      val ns = cons(0, iterate((_:Int)/2, n).takeWhile(_ != 0).map(n - _))
      if(n < 0) cons(-n,ns) else ns
    }
  }

  /** Shrink instance of Option */
  implicit def shrinkOption[T](implicit s: Shrink[T]): Shrink[Option[T]] =
    Shrink { 
      case None    => empty
      case Some(x) => cons(None, for(y <- shrink(x)) yield Some(y))
    }

  /** Shrink instance of list */
  implicit def shrinkList[T](implicit s: Shrink[T]): Shrink[List[T]] = 
    Shrink { xs =>
      def interleave(xs: Stream[List[T]],ys: Stream[List[T]]): Stream[List[T]] = 
        (xs,ys) match {
          case (xs,ys) if xs.isEmpty => ys
          case (xs,ys) if ys.isEmpty => xs
          case (cons(x,xs),cons(y,ys)) => cons(x, cons(y, interleave(xs,ys)))
        }

      def removeChunks(n: Int, xs: List[T]): Stream[List[T]] = xs match {
        case Nil => empty
        case _::Nil => cons(Nil, empty)
        case _ =>
          val n1 = n / 2
          val n2 = n - n1
          lazy val xs1 = xs.take(n1)
          lazy val xs2 = xs.drop(n1)
          lazy val xs3 =
            for(ys1 <- removeChunks(n1,xs1) if !ys1.isEmpty) yield ys1 ::: xs2
          lazy val xs4 =
            for(ys2 <- removeChunks(n2,xs2) if !ys2.isEmpty) yield xs1 ::: ys2

          cons(xs1, cons(xs2, interleave(xs3,xs4)))
      }

      def shrinkOne(xs: List[T]): Stream[List[T]] = xs match {
        case Nil => empty
        case x::xs =>
          (for(y <- shrink(x)) yield y::xs) append
          (for(ys <- shrinkOne(xs)) yield x::ys)
      }

      removeChunks(xs.length,xs).append(shrinkOne(xs))
    }

  /** Shrink instance of 2-tuple */
  implicit def shrinkTuple2[T1,T2](implicit
    s1: Shrink[T1], s2: Shrink[T2]
  ): Shrink[(T1,T2)] = 
    Shrink { case (t1,t2) =>
      (for(x1 <- shrink(t1)) yield (x1, t2)) append
      (for(x2 <- shrink(t2)) yield (t1, x2))
    }

  /** Shrink instance of 3-tuple */
  implicit def shrinkTuple3[T1,T2,T3](implicit
    s1: Shrink[T1], s2: Shrink[T2], s3: Shrink[T3]
  ): Shrink[(T1,T2,T3)] = 
    Shrink { case (t1,t2,t3) =>
      println("SHRINKING TUPLE" + (t1,t2,t3))
      (for(x1 <- shrink(t1)) yield (x1, t2, t3)) append
      (for(x2 <- shrink(t2)) yield (t1, x2, t3)) append
      (for(x3 <- shrink(t3)) yield (t1, t2, x3))
    }

  /** Shrink instance of 4-tuple */
  implicit def shrinkTuple4[T1,T2,T3,T4](implicit
    s1: Shrink[T1], s2: Shrink[T2], s3: Shrink[T3], s4: Shrink[T4]
  ): Shrink[(T1,T2,T3,T4)] = 
    Shrink { case (t1,t2,t3,t4) =>
      (for(x1 <- shrink(t1)) yield (x1, t2, t3, t4)) append
      (for(x2 <- shrink(t2)) yield (t1, x2, t3, t4)) append
      (for(x3 <- shrink(t3)) yield (t1, t2, x3, t4)) append
      (for(x4 <- shrink(t4)) yield (t1, t2, t3, x4))
    }

  /** Shrink instance of 5-tuple */
  implicit def shrinkTuple4[T1,T2,T3,T4,T5](implicit
    s1: Shrink[T1], s2: Shrink[T2], s3: Shrink[T3], s4: Shrink[T4], 
    s5: Shrink[T5]
  ): Shrink[(T1,T2,T3,T4,T5)] = 
    Shrink { case (t1,t2,t3,t4,t5) =>
      (for(x1 <- shrink(t1)) yield (x1, t2, t3, t4, t5)) append
      (for(x2 <- shrink(t2)) yield (t1, x2, t3, t4, t5)) append
      (for(x3 <- shrink(t3)) yield (t1, t2, x3, t4, t5)) append
      (for(x4 <- shrink(t4)) yield (t1, t2, t3, x4, t5)) append
      (for(x5 <- shrink(t5)) yield (t1, t2, t3, t4, x5))
    }

  /** Shrink instance of 6-tuple */
  implicit def shrinkTuple4[T1,T2,T3,T4,T5,T6](implicit
    s1: Shrink[T1], s2: Shrink[T2], s3: Shrink[T3], s4: Shrink[T4], 
    s5: Shrink[T5], s6: Shrink[T6]
  ): Shrink[(T1,T2,T3,T4,T5,T6)] = 
    Shrink { case (t1,t2,t3,t4,t5,t6) =>
      (for(x1 <- shrink(t1)) yield (x1, t2, t3, t4, t5, t6)) append
      (for(x2 <- shrink(t2)) yield (t1, x2, t3, t4, t5, t6)) append
      (for(x3 <- shrink(t3)) yield (t1, t2, x3, t4, t5, t6)) append
      (for(x4 <- shrink(t4)) yield (t1, t2, t3, x4, t5, t6)) append
      (for(x5 <- shrink(t5)) yield (t1, t2, t3, t4, x5, t6)) append
      (for(x6 <- shrink(t6)) yield (t1, t2, t3, t4, t5, x6))
    }

  /** Shrink instance of 7-tuple */
  implicit def shrinkTuple4[T1,T2,T3,T4,T5,T6,T7](implicit
    s1: Shrink[T1], s2: Shrink[T2], s3: Shrink[T3], s4: Shrink[T4], 
    s5: Shrink[T5], s6: Shrink[T6], s7: Shrink[T7]
  ): Shrink[(T1,T2,T3,T4,T5,T6,T7)] = 
    Shrink { case (t1,t2,t3,t4,t5,t6,t7) =>
      (for(x1 <- shrink(t1)) yield (x1, t2, t3, t4, t5, t6, t7)) append
      (for(x2 <- shrink(t2)) yield (t1, x2, t3, t4, t5, t6, t7)) append
      (for(x3 <- shrink(t3)) yield (t1, t2, x3, t4, t5, t6, t7)) append
      (for(x4 <- shrink(t4)) yield (t1, t2, t3, x4, t5, t6, t7)) append
      (for(x5 <- shrink(t5)) yield (t1, t2, t3, t4, x5, t6, t7)) append
      (for(x6 <- shrink(t6)) yield (t1, t2, t3, t4, t5, x6, t7)) append
      (for(x7 <- shrink(t7)) yield (t1, t2, t3, t4, t5, t6, x7))
    }

  /** Shrink instance of 8-tuple */
  implicit def shrinkTuple4[T1,T2,T3,T4,T5,T6,T7,T8](implicit
    s1: Shrink[T1], s2: Shrink[T2], s3: Shrink[T3], s4: Shrink[T4], 
    s5: Shrink[T5], s6: Shrink[T6], s7: Shrink[T7], s8: Shrink[T8]
  ): Shrink[(T1,T2,T3,T4,T5,T6,T7,T8)] = 
    Shrink { case (t1,t2,t3,t4,t5,t6,t7,t8) =>
      (for(x1 <- shrink(t1)) yield (x1, t2, t3, t4, t5, t6, t7, t8)) append
      (for(x2 <- shrink(t2)) yield (t1, x2, t3, t4, t5, t6, t7, t8)) append
      (for(x3 <- shrink(t3)) yield (t1, t2, x3, t4, t5, t6, t7, t8)) append
      (for(x4 <- shrink(t4)) yield (t1, t2, t3, x4, t5, t6, t7, t8)) append
      (for(x5 <- shrink(t5)) yield (t1, t2, t3, t4, x5, t6, t7, t8)) append
      (for(x6 <- shrink(t6)) yield (t1, t2, t3, t4, t5, x6, t7, t8)) append
      (for(x7 <- shrink(t7)) yield (t1, t2, t3, t4, t5, t6, x7, t8)) append
      (for(x8 <- shrink(t8)) yield (t1, t2, t3, t4, t5, t6, t7, x8))
    }

  /** Shrink instance of 9-tuple */
  implicit def shrinkTuple4[T1,T2,T3,T4,T5,T6,T7,T8,T9](implicit
    s1: Shrink[T1], s2: Shrink[T2], s3: Shrink[T3], s4: Shrink[T4], 
    s5: Shrink[T5], s6: Shrink[T6], s7: Shrink[T7], s8: Shrink[T8],
    s9: Shrink[T9]
  ): Shrink[(T1,T2,T3,T4,T5,T6,T7,T8,T9)] = 
    Shrink { case (t1,t2,t3,t4,t5,t6,t7,t8,t9) =>
      (for(x1 <- shrink(t1)) yield (x1, t2, t3, t4, t5, t6, t7, t8, t9)) append
      (for(x2 <- shrink(t2)) yield (t1, x2, t3, t4, t5, t6, t7, t8, t9)) append
      (for(x3 <- shrink(t3)) yield (t1, t2, x3, t4, t5, t6, t7, t8, t9)) append
      (for(x4 <- shrink(t4)) yield (t1, t2, t3, x4, t5, t6, t7, t8, t9)) append
      (for(x5 <- shrink(t5)) yield (t1, t2, t3, t4, x5, t6, t7, t8, t9)) append
      (for(x6 <- shrink(t6)) yield (t1, t2, t3, t4, t5, x6, t7, t8, t9)) append
      (for(x7 <- shrink(t7)) yield (t1, t2, t3, t4, t5, t6, x7, t8, t9)) append
      (for(x8 <- shrink(t8)) yield (t1, t2, t3, t4, t5, t6, t7, x8, t9)) append
      (for(x9 <- shrink(t9)) yield (t1, t2, t3, t4, t5, t6, t7, t8, x9))
    }
}
