/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2008 Rickard Nilsson. All rights reserved.          **
**  http://code.google.com/p/scalacheck/                                   **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*-------------------------------------------------------------------------*/

package org.scalacheck

sealed trait Arbitrary[T] {
  def arbitrary: Gen[T]
}

/** Defines implicit <code>Arbitrary</code> instances for common types.
 *  <p>
 *  ScalaCheck
 *  uses implicit <code>Arbitrary</code> instances when creating properties
 *  out of functions with the <code>Prop.property</code> method, and when
 *  the <code>Arbitrary.arbitrary</code> method is used. For example, the
 *  following code requires that there exists an implicit
 *  <code>Arbitrary[MyClass]</code> instance:
 *  </p>
 *
 *  <p>
 *  <code>
 *    val myProp = Prop.property { myClass: MyClass =&gt;
 *      ...
 *    }
 *
 *    val myGen = Arbitrary.arbitrary[MyClass]
 *  </code>
 *  </p>
 *
 *  <p>
 *  The required implicit definition could look like this:
 *  </p>
 *
 *  <p>
 *  <code>
 *    implicit val arbMyClass: Arbitrary[MyClass] =
 *      Arbitrary(...)
 *  </code>
 *  </p>
 *
 *  <p>
 *  The factory method <code>Arbitrary(...)</code> takes a generator of type
 *  <code>Gen[T]</code> and returns an instance of <code>Arbitrary[T]</code>.
 *  </p>
 *
 *  <p>
 *  The <code>Arbitrary</code> module defines implicit <code>Arbitrary</code>
 *  instances for common types, for convenient use in your properties and
 *  generators.
 *  </p>
 */
object Arbitrary {

  import Gen.{value, choose, sized, elements, listOf, listOf1,
    frequency, oneOf, elementsFreq, containerOf}

  /** Creates an Arbitrary instance */
  def apply[T](g: => Gen[T]): Arbitrary[T] = new Arbitrary[T] {
    override def arbitrary = g
  }

  /** Returns an arbitrary generator for the type T. */
  def arbitrary[T](implicit a: Arbitrary[T]): Gen[T] = a.arbitrary


  // Arbitrary instances for common types //


  // Primitive types //

  /** Arbitrary instance of bool */
  implicit lazy val arbBool: Arbitrary[Boolean] =
    Arbitrary(elements(true,false))

  /** Arbitrary instance of integer */
  implicit lazy val arbInt: Arbitrary[Int] =
    Arbitrary(sized(s => choose(-s,s)))

  /** Arbitrary instance of Throwable */
  implicit lazy val arbThrowable: Arbitrary[Throwable] =
    Arbitrary(value(new Exception))

  /** Arbitrary instance of Double */
  implicit lazy val arbDouble: Arbitrary[Double] =
    Arbitrary(sized(s => choose(-s:Double,s:Double)))

  /** Arbitrary instance of char */
  implicit lazy val arbChar: Arbitrary[Char] =
    Arbitrary(choose(0,255).map(_.toChar))

  /** Arbitrary instance of byte */
  implicit lazy val arbByte: Arbitrary[Byte] =
    Arbitrary(arbitrary[Int].map(_.toByte))

  /** Arbitrary instance of string */
  implicit lazy val arbString: Arbitrary[String] =
    Arbitrary(arbitrary[List[Char]].map(List.toString(_)))

  /** Generates an arbitrary property */
  implicit lazy val arbProp: Arbitrary[Prop] =
    Arbitrary(elementsFreq(
      (5, Prop.proved),
      (4, Prop.falsified),
      (2, Prop.undecided),
      (1, Prop.exception(null))
    ))

  /** Arbitrary instance of test params */
  implicit lazy val arbTestParams: Arbitrary[Test.Params] =
    Arbitrary(for {
      minSuccTests <- choose(10,150)
      maxDiscTests <- choose(100,500)
      minSize <- choose(0,500)
      sizeDiff <- choose(0,500)
      maxSize <- choose(minSize, minSize + sizeDiff)
    } yield Test.Params(minSuccTests,maxDiscTests,minSize,maxSize,StdRand))

  /** Arbitrary instance of gen params */
  implicit lazy val arbGenParams: Arbitrary[Gen.Params] =
    Arbitrary(for {
      size <- arbitrary[Int] suchThat (_ >= 0)
    } yield Gen.Params(size, StdRand))


  // Higher-order types //

  /** Arbitrary instance of Gen */
  implicit def arbGen[T](implicit a: Arbitrary[T]): Arbitrary[Gen[T]] =
    Arbitrary(frequency(
      (5, arbitrary[T] map (value(_))),
      (1, Gen.fail)
    ))

  /** Arbitrary instance of option type */
  implicit def arbOption[T](implicit a: Arbitrary[T]): Arbitrary[Option[T]] =
    Arbitrary(oneOf(value(None), arbitrary[T].map(Some(_))))

  /** Arbitrary instance of any buildable container (such as lists, arrays, 
   *  streams, etc). The maximum size of the container depends on the size 
   *  generation parameter. */
  //implicit def arbContainer[C[_],T](implicit a: Arbitrary[T], b: Buildable[C]
  //): Arbitrary[C[T]] =
  //  Arbitrary(containerOf[C,T](arbitrary[T]))

  // The above code crashes in Scala 2.7, therefore we must explicitly define
  // the arbitrary containers for each supported type below.

  implicit def arbList[T](implicit a: Arbitrary[T]): Arbitrary[List[T]] =
    Arbitrary(containerOf[List,T](arbitrary[T]))

  implicit def arbStream[T](implicit a: Arbitrary[T]): Arbitrary[Stream[T]] =
    Arbitrary(containerOf[Stream,T](arbitrary[T]))

  implicit def arbArray[T](implicit a: Arbitrary[T]): Arbitrary[Array[T]] =
    Arbitrary(containerOf[Array,T](arbitrary[T]))

  import java.util.ArrayList
  implicit def arbArrayList[T](implicit a: Arbitrary[T]): Arbitrary[ArrayList[T]] =
    Arbitrary(containerOf[ArrayList,T](arbitrary[T]))


  // Tuples //

  /** Arbitrary instance of 2-tuple */
  implicit def arbTuple2[T1,T2](implicit
    a1: Arbitrary[T1], a2: Arbitrary[T2]
  ): Arbitrary[(T1,T2)] =
    Arbitrary(for {
      t1 <- arbitrary[T1]
      t2 <- arbitrary[T2]
    } yield (t1,t2))

  /** Arbitrary instance of 3-tuple */
  implicit def arbTuple3[T1,T2,T3](implicit
    a1: Arbitrary[T1], a2: Arbitrary[T2], a3: Arbitrary[T3]
  ): Arbitrary[(T1,T2,T3)] =
    Arbitrary(for {
      t1 <- arbitrary[T1]
      t2 <- arbitrary[T2]
      t3 <- arbitrary[T3]
    } yield (t1,t2,t3))

  /** Arbitrary instance of 4-tuple */
  implicit def arbTuple4[T1,T2,T3,T4](implicit
    a1: Arbitrary[T1], a2: Arbitrary[T2], a3: Arbitrary[T3], a4: Arbitrary[T4]
  ): Arbitrary[(T1,T2,T3,T4)] =
    Arbitrary(for {
      t1 <- arbitrary[T1]
      t2 <- arbitrary[T2]
      t3 <- arbitrary[T3]
      t4 <- arbitrary[T4]
    } yield (t1,t2,t3,t4))

  /** Arbitrary instance of 5-tuple */
  implicit def arbTuple5[T1,T2,T3,T4,T5](implicit
    a1: Arbitrary[T1], a2: Arbitrary[T2], a3: Arbitrary[T3], a4: Arbitrary[T4],
    a5: Arbitrary[T5]
  ): Arbitrary[(T1,T2,T3,T4,T5)] =
    Arbitrary(for {
      t1 <- arbitrary[T1]
      t2 <- arbitrary[T2]
      t3 <- arbitrary[T3]
      t4 <- arbitrary[T4]
      t5 <- arbitrary[T5]
    } yield (t1,t2,t3,t4,t5))

  /** Arbitrary instance of 6-tuple */
  implicit def arbTuple6[T1,T2,T3,T4,T5,T6](implicit
    a1: Arbitrary[T1], a2: Arbitrary[T2], a3: Arbitrary[T3], a4: Arbitrary[T4],
    a5: Arbitrary[T5], a6: Arbitrary[T6]
  ): Arbitrary[(T1,T2,T3,T4,T5,T6)] =
    Arbitrary(for {
      t1 <- arbitrary[T1]
      t2 <- arbitrary[T2]
      t3 <- arbitrary[T3]
      t4 <- arbitrary[T4]
      t5 <- arbitrary[T5]
      t6 <- arbitrary[T6]
    } yield (t1,t2,t3,t4,t5,t6))

  /** Arbitrary instance of 7-tuple */
  implicit def arbTuple6[T1,T2,T3,T4,T5,T6,T7](implicit
    a1: Arbitrary[T1], a2: Arbitrary[T2], a3: Arbitrary[T3], a4: Arbitrary[T4],
    a5: Arbitrary[T5], a6: Arbitrary[T6], a7: Arbitrary[T7]
  ): Arbitrary[(T1,T2,T3,T4,T5,T6,T7)] =
    Arbitrary(for {
      t1 <- arbitrary[T1]
      t2 <- arbitrary[T2]
      t3 <- arbitrary[T3]
      t4 <- arbitrary[T4]
      t5 <- arbitrary[T5]
      t6 <- arbitrary[T6]
      t7 <- arbitrary[T7]
    } yield (t1,t2,t3,t4,t5,t6,t7))

  /** Arbitrary instance of 8-tuple */
  implicit def arbTuple6[T1,T2,T3,T4,T5,T6,T7,T8](implicit
    a1: Arbitrary[T1], a2: Arbitrary[T2], a3: Arbitrary[T3], a4: Arbitrary[T4],
    a5: Arbitrary[T5], a6: Arbitrary[T6], a7: Arbitrary[T7], a8: Arbitrary[T8]
  ): Arbitrary[(T1,T2,T3,T4,T5,T6,T7,T8)] =
    Arbitrary(for {
      t1 <- arbitrary[T1]
      t2 <- arbitrary[T2]
      t3 <- arbitrary[T3]
      t4 <- arbitrary[T4]
      t5 <- arbitrary[T5]
      t6 <- arbitrary[T6]
      t7 <- arbitrary[T7]
      t8 <- arbitrary[T8]
    } yield (t1,t2,t3,t4,t5,t6,t7,t8))

  /** Arbitrary instance of 9-tuple */
  implicit def arbTuple6[T1,T2,T3,T4,T5,T6,T7,T8,T9](implicit
    a1: Arbitrary[T1], a2: Arbitrary[T2], a3: Arbitrary[T3], a4: Arbitrary[T4],
    a5: Arbitrary[T5], a6: Arbitrary[T6], a7: Arbitrary[T7], a8: Arbitrary[T8],
    a9: Arbitrary[T9]
  ): Arbitrary[(T1,T2,T3,T4,T5,T6,T7,T8,T9)] =
    Arbitrary(for {
      t1 <- arbitrary[T1]
      t2 <- arbitrary[T2]
      t3 <- arbitrary[T3]
      t4 <- arbitrary[T4]
      t5 <- arbitrary[T5]
      t6 <- arbitrary[T6]
      t7 <- arbitrary[T7]
      t8 <- arbitrary[T8]
      t9 <- arbitrary[T9]
    } yield (t1,t2,t3,t4,t5,t6,t7,t8,t9))

}
