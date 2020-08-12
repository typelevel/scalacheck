package org.scalacheck

import rng.Seed
import scala.reflect.runtime.universe._
import Gen._
import Prop.{forAll, someFailing, noneFailing, sizedProp, secure, propBoolean}
import Arbitrary._
import Shrink._
import org.scalacheck.util.Buildable
import java.io.{
  ByteArrayInputStream,
  ByteArrayOutputStream,
  ObjectInputStream,
  ObjectOutputStream
}
import java.util.Date
import scala.util.{Try, Success, Failure}
import scala.concurrent.duration.Duration

object SerializationSpecification extends Properties("Serialization") {
  def testSerializable(obj: Any): Prop = {
    val t = Try({
      val baos = new ByteArrayOutputStream()
      val oos = new ObjectOutputStream(baos)
      oos.writeObject(obj)
      oos.close()

      // Reading it back in shouldn't throw, but we don't expect equality.
      val bais = new ByteArrayInputStream(baos.toByteArray())
      new ObjectInputStream(bais).readObject()
      ()
    })
    t match {
      case Success(_) => Prop(true)
      case _          => Prop(false) :| s"Fail to serialize $obj"
    }
  }

  def testSerializableGenAndValues[A](gen: Gen[A]): Prop =
    testSerializable(gen) && forAll(gen)(testSerializable)

  def testArbitraryAndCogen[A](implicit arb: Arbitrary[A], cogen: Cogen[A]): Prop =
    testSerializable(cogen) && testSerializableGenAndValues(arb.arbitrary)

  def registerTest[A](implicit tag: TypeTag[A], arb: Arbitrary[A], cogen: Cogen[A]): Unit = {
    val typeName = tag.tpe.toString
    property(s"Cogen[$typeName], arbitrary[$typeName] and its produced values are serializable") = testArbitraryAndCogen[A]
  }

  registerTest[(String, Int)]

  registerTest[(String, Int, Int)]

  registerTest[List[Int] => List[(String, Int)]]

  property("Buildable[Int, List[Int]] is serializable") = testSerializable(
    implicitly[Buildable[Int, List[Int]]]
  )

  registerTest[List[Int]]

  registerTest[List[(String, Int)]]

  registerTest[List[(String, Int)] => List[Int]]

  registerTest[List[(Int, (String, Int))] => List[(String, Int)]]

  registerTest[Set[Int]]

  registerTest[Duration]

  registerTest[Try[Option[Int]]]
}
