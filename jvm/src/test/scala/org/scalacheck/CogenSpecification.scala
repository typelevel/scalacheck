package org.scalacheck

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen.listOfN
import org.scalacheck.GenSpecification.arbSeed
import org.scalacheck.Prop.forAll
import org.scalacheck.rng.Seed
import ScalaVersionSpecific._


import scala.util.Try
import scala.concurrent.duration.{Duration, FiniteDuration}

object CogenSpecification extends Properties("Cogen") {

  // We need a customized definition of equality for some of our tests.
  trait Equal[A] {
    def equal(a1: A, a2: A): Boolean
  }

  object Equal extends EqualLowPriority {

    def apply[A: Equal]: Equal[A] = implicitly

    // Value equality for arrays as opposed to reference equality, consistent with other sequences.
    implicit def arrayEqual[A: Equal]: Equal[Array[A]] = new Equal[Array[A]] {
      override def equal(a1: Array[A], a2: Array[A]) =
        a1.corresponds(a2)(Equal[A].equal)
    }

    // Two thunks are equal if evaluating them results in the same value.
    implicit def function0Equal[A: Equal]: Equal[() => A] = new Equal[() => A] {
      override def equal(a1: () => A, a2: () => A) =
        Equal[A].equal(a1(), a2())
    }

    // Two exceptions are equal if they have the same string representation.
    implicit val exceptionEqual: Equal[Exception] = new Equal[Exception] {
      override def equal(a1: Exception, a2: Exception): Boolean =
        a1.toString == a2.toString
    }

    // Two throwables are equal if they have the same string representation.
    implicit val throwableEqual: Equal[Throwable] = new Equal[Throwable] {
      override def equal(a1: Throwable, a2: Throwable): Boolean =
        a1.toString == a2.toString
    }

    // Two cogens are equal if for all combinations of seeds and values they product the same result.
    implicit def cogenEqual[A: Arbitrary]: Equal[Cogen[A]] = new Equal[Cogen[A]] {
      def equal(a1: Cogen[A], a2: Cogen[A]): Boolean =
        listOfN(100, arbitrary[(Seed, A)]).sample.get.forall {
          case (x, y) => a1.perturb(x, y) == a2.perturb(x, y)
        }
    }
  }

  // Avoid reimplementing equality for other standard classes.
  trait EqualLowPriority {
    implicit def universal[A]: Equal[A] = new Equal[A] {
      override def equal(a1: A, a2: A): Boolean = a1 == a2
    }
  }

  // A version of distinct that accepts a custom notion of equality.
  def distinct[A: Equal](as: List[A]): List[A] =
    as.foldLeft(List.empty[A])((b, a) => if (b.exists(Equal[A].equal(a, _))) b else a :: b)

  implicit def arbFunction0[A: Arbitrary]: Arbitrary[() => A] =
    Arbitrary(arbitrary[A].map(() => _))

  implicit def arbCogen[A: Arbitrary : Cogen]: Arbitrary[Cogen[A]] =
    Arbitrary(arbitrary[A => A].map(Cogen[A].contramap(_)))

  // Cogens should follow these laws.
  object CogenLaws {

    /*
    A cogen should always generate different outputs for inputs that are not equal.
    Note that since we are using pseudorandom number generation this is only required approximately.
    In particular, if the space of the input is larger than the space of the seed (2^256) there is a
    possibility of legitimate collisions, but this is vanishingly small for the sample sizes we are using.
     */
    def uniqueness[A: Equal : Arbitrary : Cogen]: Prop =
      forAll { (seed: Seed, as: List[A]) =>
        as.map(Cogen[A].perturb(seed, _)).toSet.size == distinct(as).size
      }

    // A Cogen should always generate the same output for a given seed and input.
    def consistency[A: Arbitrary : Cogen]: Prop =
      forAll { (seed: Seed, a: A) =>
        Cogen[A].perturb(seed, a) == Cogen[A].perturb(seed, a)
      }
  }

  def cogenLaws[A: Equal : Arbitrary : Cogen]: Properties =
    new Properties("cogenLaws") {
      property("uniqueness") = CogenLaws.uniqueness[A]
      property("consistency") = CogenLaws.consistency[A]
    }

  // A Cogen is a contravariant functor and should follow the laws for contravariant functors.
  object ContravariantLaws {

    // Contramapping over a Cogen with the identity function should return the Cogen unchanged.
    def identity[A: Equal : Arbitrary : Cogen]: Prop =
      forAll { (fa: Cogen[A]) =>
        Equal[Cogen[A]].equal(fa.contramap(a => a), fa)
      }

    // Contramapping with f and g is the same as contramapping with the composition of f and g.
    def composition[A, B, C](implicit eq: Equal[Cogen[C]],
                             arb1: Arbitrary[Cogen[A]],
                             arb2: Arbitrary[B => A],
                             arb3: Arbitrary[C => B]): Prop =
      forAll { (fa: Cogen[A], f: B => A, g: C => B) =>
        Equal[Cogen[C]].equal(fa.contramap(f).contramap(g), fa.contramap(f compose g))
      }
  }

  def contravariantLaws: Properties =
    new Properties("contravariantLaws") {
      property("identity") = ContravariantLaws.identity[Int]
      property("composition") = ContravariantLaws.composition[Int, Int, Int]
    }

  include(contravariantLaws)

  include(cogenLaws[Unit], "cogenUnit.")
  include(cogenLaws[Boolean], "cogenBoolean.")
  include(cogenLaws[Byte], "cogenByte.")
  include(cogenLaws[Short], "cogenShort.")
  include(cogenLaws[Char], "cogenChar.")
  include(cogenLaws[Int], "cogenInt.")
  include(cogenLaws[Long], "cogenLong.")
  include(cogenLaws[Float], "cogenFloat.")
  include(cogenLaws[BigInt], "cogenBigInt.")
  include(cogenLaws[BigDecimal], "cogenBigDecimal.")
  include(cogenLaws[Option[Int]], "cogenOption.")
  include(cogenLaws[Either[Int, Int]], "cogenEither.")
  include(cogenLaws[Array[Int]], "cogenArray.")
  include(cogenLaws[String], "cogenString.")
  include(cogenLaws[List[Int]], "cogenList.")
  include(cogenLaws[Vector[Int]], "cogenVector.")
  include(cogenLaws[Stream[Int]], "cogenStream.")
  include(cogenLaws[LazyList[Int]], "cogenLazyList.")
  include(cogenLaws[Set[Int]], "cogenSet.")
  include(cogenLaws[Map[Int, Int]], "cogenMap.")
  include(cogenLaws[() => Int], "cogenFunction0.")
  include(cogenLaws[Exception], "cogenException.")
  include(cogenLaws[Throwable], "cogenThrowable.")
  include(cogenLaws[Try[Int]], "cogenTry.")
  include(cogenLaws[Seq[Int]], "cogenSeq.")
  include(cogenLaws[Duration], "cogenDuration.")
  include(cogenLaws[FiniteDuration], "cogenFiniteDuration.")
}
