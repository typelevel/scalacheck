package org.scalacheck

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream }

import Prop.proved

import util.SerializableCanBuildFroms._

object SerializabilitySpecification extends Properties("Serializability") {

  // adapted from https://github.com/milessabin/shapeless/blob/6b870335c219d59079b46eddff15028332c0c294/core/jvm/src/test/scala/shapeless/serialization.scala#L42-L62
  private def serializable[M](m: M): Boolean = {
    val baos = new ByteArrayOutputStream
    val oos = new ObjectOutputStream(baos)
    var ois: ObjectInputStream = null
    try {
      oos.writeObject(m)
      oos.close()
      val bais = new ByteArrayInputStream(baos.toByteArray)
      ois = new ObjectInputStream(bais)
      val m2 = ois.readObject() // just ensure we can read it back
      ois.close()
      true
    } catch {
      case thr: Throwable =>
        thr.printStackTrace
        false
    } finally {
      oos.close()
      if (ois != null) ois.close()
    }
  }

  def serializableArbitrary[T: Arbitrary](name: String) =
    property(s"Arbitrary[$name] serializability") = {
      val arb = implicitly[Arbitrary[T]]
      assert(serializable(arb))

      // forcing the calculation of a value, to trigger the initialization of any lazily initialized field
      arb.arbitrary.sample
      assert(serializable(arb))

      proved
    }

  def serializableGen[T](name: String, gen: Gen[T]) =
    property(s"Gen[$name] serializability") = {
      assert(serializable(gen))

      // forcing the calculation of a value, to trigger the initialization of any lazily initialized field
      gen.sample
      assert(serializable(gen))

      proved
    }

  def serializableCogen[T: Cogen](name: String) =
    property(s"Cogen[$name] serializability") = {
      val gen = Cogen[T]
      assert(serializable(gen))

      proved
    }

  def serializableShrink[T: Shrink](name: String) =
    property(s"Shrink[$name] serializability") = {
      val shrink = implicitly[Shrink[T]]
      assert(serializable(shrink))

      proved
    }

  serializableArbitrary[String]("String")
  serializableArbitrary[Int]("Int")
  serializableArbitrary[Double]("Double")
  serializableArbitrary[Boolean]("Boolean")

  serializableGen("identifier", Gen.identifier)
  serializableGen("oneOf", Gen.oneOf(true, false))
  serializableGen("choose", Gen.choose(1, 10))

  serializableCogen[String]("String")
  serializableCogen[Int]("Int")
  serializableCogen[Double]("Double")
  serializableCogen[Boolean]("Boolean")

  serializableShrink[String]("String")
  serializableShrink[Int]("Int")
  serializableShrink[Double]("Double")
  serializableShrink[Boolean]("Boolean")

  property("Seed serializability") = {
    assert(serializable(rng.Seed(1L)))
    proved
  }

}