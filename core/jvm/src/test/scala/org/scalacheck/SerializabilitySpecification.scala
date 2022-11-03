/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import org.apache.commons.lang3.SerializationUtils
import java.io.Serializable

import util.SerializableCanBuildFroms._

object SerializabilitySpecification extends Properties("Serializability") {

  def serializable[M <: Serializable](m: M): Boolean = {
    SerializationUtils.roundtrip(m)
    true
  }

  def serializableArbitrary[T: Arbitrary](name: String) =
    property(s"Arbitrary[$name]") = {
      val arb = implicitly[Arbitrary[T]]
      serializable(arb)

      // forcing the calculation of a value, to trigger the initialization of any lazily initialized field
      arb.arbitrary.sample
      serializable(arb)
    }

  def serializableGen[T](name: String, gen: Gen[T]) =
    property(name) = {
      serializable(gen)

      // forcing the calculation of a value, to trigger the initialization of any lazily initialized field
      gen.sample
      serializable(gen)
    }

  def serializableCogen[T: Cogen](name: String) =
    property(s"Cogen[$name]") = {
      val gen = Cogen[T]
      serializable(gen)
    }

  def serializableShrink[T: Shrink](name: String) =
    property(s"Shrink[$name]") = {
      val shrink = implicitly[Shrink[T]]
      serializable(shrink)
    }

  serializableArbitrary[String]("String")
  serializableArbitrary[Int]("Int")
  serializableArbitrary[Double]("Double")
  serializableArbitrary[Boolean]("Boolean")
  serializableArbitrary[Int => Int]("Int => Int")
  serializableArbitrary[List[Int]]("List[Int]")
  serializableArbitrary[(String,Int)]("(String,Int)")
  serializableArbitrary[(Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int)]("Tuple22[Int]")
  serializableArbitrary[List[(String,Int)]]("List[(String,Int)]")

  serializableGen("Gen.identifier", Gen.identifier)
  serializableGen("Gen.oneOf", Gen.oneOf(true, false))
  serializableGen("Gen.choose", Gen.choose(1, 10))
  serializableGen("Gen.function1", Gen.function1[Int, Int](Gen.choose(1, 10)))
  serializableGen("Gen.zip(String,Int)", Gen.zip(Arbitrary.arbitrary[String], Arbitrary.arbitrary[Int]))

  serializableCogen[String]("String")
  serializableCogen[Int]("Int")
  serializableCogen[Double]("Double")
  serializableCogen[Boolean]("Boolean")
  serializableCogen[Int => Int]("Int => Int")
  serializableCogen[List[Int]]("List[Int]")
  serializableCogen[(String,Int)]("(String,Int)")
  serializableCogen[(Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int)]("Tuple22[Int]")
  serializableCogen[List[(String,Int)]]("List[(String,Int)]")

  serializableShrink[String]("String")
  serializableShrink[Int]("Int")
  serializableShrink[Double]("Double")
  serializableShrink[Boolean]("Boolean")
  serializableShrink[Int => Int]("Int => Int")
  serializableShrink[List[Int]]("List[Int]")
  serializableShrink[(String,Int)]("(String,Int)")
  serializableShrink[(Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int)]("Tuple22[Int])")
  serializableShrink[List[(String,Int)]]("List[(String,Int)]")

  property("Seed(1L)") = {
    serializable(rng.Seed(1L))
  }

}
