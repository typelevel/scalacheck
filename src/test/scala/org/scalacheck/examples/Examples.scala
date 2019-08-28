package org.scalacheck.examples

import org.scalacheck.{Prop, Properties}

object Examples extends Properties("Examples") {

  property("list tail") = Prop.forAll { (n: Int, l: List[Int]) =>
    (n :: l).tail == l
  }

  property("list head") = Prop.forAll { l: List[Int] =>
    if (l.isEmpty) {
      Prop.throws(classOf[java.util.NoSuchElementException]) { l.head }
    } else {
      l.head == l(0)
    }
  }

  case class Person (
    firstName: String,
    lastName: String,
    age: Int 
  ) { 
    def isTeenager = age >= 13 && age <= 19
  } 

  val genPerson = {
    import org.scalacheck.Gen.{choose, oneOf}
    for {
      firstName <- oneOf("Alan", "Ada", "Alonzo")
      lastName <- oneOf("Lovelace", "Turing", "Church")
      age <- choose(1,100)
    } yield Person(firstName, lastName, age)
  }

  import org.scalacheck.Arbitrary

  implicit val arbPerson: Arbitrary[Person] = Arbitrary(genPerson)

  property("ex1") = Prop.forAll { p: Person =>
    p.isTeenager == (p.age >= 13 && p.age <= 19)
  }

}
