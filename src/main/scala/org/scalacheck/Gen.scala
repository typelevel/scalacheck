/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import language.higherKinds
import language.implicitConversions

import rng.Seed
import util.Buildable
import util.SerializableCanBuildFroms._
import ScalaVersionSpecific._

import scala.annotation.tailrec
import scala.collection.immutable.TreeMap
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.{Duration, FiniteDuration}

import java.util.{ Calendar, UUID }
import java.math.{BigInteger, BigDecimal => JavaDecimal}

/**
 * A generator produces values for [[Prop]]s
 *
 * This module provides:
 *  1. Definitions for non-arbitrary generators,
 *  1. Factories to construct generators,
 *  1. Methods to modify a generator, and
 *  1. Various combinators for producing generators of values for more
 *     complex data types.
 *
 * Explicit generators aren't required to write [[Prop]]s:
 *
 * {{{
 * Prop.forAll { (n: Int) =>
 *   n == n
 * }
 * }}}
 *
 * The [[Prop]] above is defined with parameters only and without an
 * explicit generator, because generators are implicitly provided by
 * [[Arbitrary]] for various data types.
 *
 * However, it's not uncommon to need to write explicit custom
 * generators:
 *
 * {{{
 * val genInt: Gen[Int] = Gen.choose(1,10)
 * Prop.forAll(genInt) { (n: Int) =>
 *   1 <= n && n <= 10
 * }
 * }}}
 *
 * This is a simple definition of a generator for booleans:
 * {{{
 * val genBool: Gen[Boolean] = Gen.oneOf(true,false)
 * }}}
 *
 * The above definition isn't necessary, though.  The same boolean
 * generator is defined in [[Arbitrary]] as an implicit declaration
 * for automatically parameterizing [[Prop]]s.  Instead, use the
 * generator that is defined in [[Arbitrary]] and available from the
 * polymorphic method [[Arbitrary.arbitrary]] with an explicit type
 * parameter:
 *
 * {{{
 * val genBool: Gen[Boolean] = Arbitrary.arbitrary[Boolean]
 * }}}
 *
 * Alternatively, this is a boolean generator, but one that always
 * produces true:
 * {{{
 * val genBool = Gen.const(true)
 * }}}
 *
 * This is a generator of booleans that is true at a 2-to-1 ratio:
 * {{{
 * val genBool = Gen.frequency(2 -> true, 1 -> false)
 * }}}
 *
 * This is a boolean generator that will produce true 75% of the time:
 * {{{
 * val genBool = Gen.prob(0.75)
 * }}}
 *
 * For more information on designing custom generators and the
 * motivations for doing so, see chapter 6, ''Generators in Detail'',
 * of the book ''ScalaCheck: The Definitive Guide'' (2013) by Rickard
 * Nilsson published by Artima Press.
 *
 * This is an example of a custom generator for integers:
 * {{{
 * val genSmallInt: Gen[Int] = Gen.choose(-100,100)
 * }}}
 *
 * This can be used to generate different collections of zero or more small integers:
 * {{{
 * val genListOfInts: Gen[List[Int]] = Gen.listOf(genSmallInt)
 *
 * val genSeqOfInts: Gen[Seq[Int]] = Gen.someOf(-100 to 100)
 *
 * val genVectorOfInts: Gen[Vector[Int]] = Gen.containerOf[Vector,Int](genSmallInt)
 *
 * val genMap: Gen[Map[Int,Boolean]] = Gen.mapOf(Gen.zip(genSmallInt, genBool))
 *
 * val genOptionalInt: Gen[Option[Int]] = Gen.option(genSmallInt)
 * }}}
 *
 * Or collections of one or more small integers:
 * {{{
 * val genListOfInts: Gen[List[Int]] = Gen.nonEmptyListOf(genSmallInt)
 *
 * val genSeqOfInts: Gen[Seq[Int]] = Gen.atLeastOne(-100 to 100)
 *
 * val genVectorOfInts: Gen[Vector[Int]] = Gen.nonEmptyContainerOf[Vector,Int](genSmallInt)
 *
 * val genMap: Gen[Map[Int,Boolean]] = Gen.nonEmptyMap(Gen.zip(genSmallInt, genBool))
 *
 * val genOptionalInt: Gen[Option[Int]] = Gen.some(genSmallInt)
 * }}}
 *
 * The class methods for [[Gen]] should be familiar with those in the
 * [[scala.collection Scala collections API]]:
 *
 *  - [[map]] - Apply a function to generated values
 *  - [[flatMap]] - Apply a function that returns a generator
 *  - [[filter]] - Use values that satisfy a predicate
 *
 * The [[Gen]] class also supports for-comprehensions to compose
 * complex generators:
 *
 * {{{
 * val genPerson = for {
 *   firstName <- Gen.oneOf("Alan", "Ada", "Alonzo")
 *   lastName <- Gen.oneOf("Lovelace", "Turing", "Church")
 *   age <- Gen.choose(0,100) if (age >= 18)
 * } yield Person(firstName, lastName, age)
 * }}}
 *
 * Constructors and factories for generators:
 *  - [[Gen$.const const]] - Always generates a single value
 *  - [[Gen$.oneOf[T](t0* oneOf]] - Generate a value from a list of values
 *  - [[Gen$.atLeastOne[T](g1* atLeastOne]] - Generate a collection with at least one value from a list
 *  - [[Gen$.someOf[T](l* someOf]] - Generate a collection with zero or more values from a list
 *  - [[Gen$.choose choose]] - Generate numeric values in an (inclusive) range
 *  - [[Gen$.frequency frequency]] - Choose from multiple values with a weighted distribution
 *
 * Combinators of generators:
 *  - [[Gen$.buildableOf buildableOf]] - Generates a collection with a generator
 *  - [[Gen$.buildableOfN buildableOfN]] - Generates a collection of at most ''n'' elements
 *  - [[Gen$.nonEmptyBuildableOf nonEmptyBuildableOf]] - Generates a non-empty collection
 *  - [[Gen$.containerOf containerOf]] - Generates a collection with a generator
 *  - [[Gen$.containerOfN containerOfN]] - Generates a collection of at most ''n'' elements
 *  - [[Gen$.nonEmptyContainerOf nonEmptyContainerOf]] - Generates a non-empty collection
 *  - [[Gen$.either either]] - Generate a disjoint union of [[scala.util.Either]]
 *  - infiniteLazyList - Generates an infinite lazy list
 *  - [[Gen$.infiniteStream infiniteStream]] - Generates an infinite stream
 *  - [[Gen$.listOf listOf]] - Generates a list of random length
 *  - [[Gen$.listOfN listOfN]] - Generates a list of at most ''n'' elements
 *  - [[Gen$.nonEmptyListOf nonEmptyListOf]] - Generates a non-empty list of random length.
 *  - [[Gen$.mapOf mapOf]] - Generates a [[scala.collection.Map]]
 *  - [[Gen$.mapOfN mapOfN]] - Generates a [[scala.collection.Map]] with at most ''n'' elements
 *  - [[Gen$.nonEmptyMap nonEmptyMap]] - Generates a non-empty map of random length
 *  - [[Gen$.option option]] - Generate values of [[scala.Some]] and [[scala.None]]
 *  - [[Gen$.pick[T](n:Int,g1* pick]] - A generator that randomly picks ''n'' elements from a list
 *  - [[Gen$.sequence sequence]] - Sequences generators.
 *  - [[Gen$.some some]] - A generator of [[scala.Some]]
 *  - [[Gen.someOf[T](g1* someOf]] - A generator that picks a random number of elements from a list
 *  - [[Gen$.stringOf stringOf]] - Generate string of characters
 *  - [[Gen$.stringOfN stringOfN]] - Generate string of at most ''n'' characters
 *
 * Methods for working with [[Gen]] internals:
 *  - [[Gen$.resize resize]] - Creates a resized version of a generator
 *  - [[Gen$.parameterized parameterized]] - Generator with the parameters
 *  - [[Gen$.size size]] - Generate with the value of the default size parameter
 *  - [[Gen$.sized sized]] - Build a generator using the default size parameter

 * Methods for probabilistic generators:
 *  - [[Gen$.exponential exponential]] - Generate numbers according to an exponential distribution
 *  - [[Gen$.gaussian gaussian]] - Generates numbers according to a Gaussian distribution
 *  - [[Gen$.geometric geometric]] - Generates numbers according to a geometric distribution
 *  - [[Gen$.poisson poisson]] - Generates numbers according to a Poisson distribution
 *  - [[Gen$.prob prob]] - Generates a boolean for the probability of true
 *
 * Definitions for generating various, non-arbitrary, common values of
 * strings and characters:
 *  - [[Gen$.alphaChar alphaChar]] - Generates an alpha character
 *  - [[Gen$.alphaStr alphaStr]] - Generates a string of alpha characters
 *  - [[Gen$.numChar numChar]] - Generates a numerical character
 *  - [[Gen$.numStr numStr]] - Generates a string of digits
 *  - [[Gen$.alphaNumChar alphaNumChar]] - Generates an alphanumerical character
 *  - [[Gen$.alphaNumStr alphaNumStr]] - Generates a string of alphanumerical characters
 *  - [[Gen$.alphaLowerChar alphaLowerChar]] - Generates a lower-case alpha character
 *  - [[Gen$.alphaLowerStr alphaLowerStr]] - Generates a string of lower-case alpha characters
 *  - [[Gen$.alphaUpperChar alphaUpperChar]] - Generates an upper-case alpha character
 *  - [[Gen$.alphaUpperStr alphaUpperStr]] - Generates a string of upper-case alpha characters
 *  - [[Gen$.asciiChar asciiChar]] - Generates an ASCII character
 *  - [[Gen$.asciiStr asciiStr]] - Generates a string of ASCII characters
 *  - [[Gen$.identifier identifier]] - Generates an identifier
 *  - [[Gen$.uuid uuid]] - Generates a UUID
 *  - [[Gen$.hexChar hexChar]] - Generates a character of a hexadecimal digit
 *  - [[Gen$.hexStr hexStr]] - Generates a string of hexadecimal digits
 *
 * Definitions for generating arbitrary values of commonly used types in
 * Scala are defined elsewhere, see [[Arbitrary]].
 *
 * There are a couple of factory methods that are for advanced uses of generators:
 *  - [[Gen$.delay delay]] - Generate a value of an expression by-name
 *  - [[Gen$.lzy lzy]] - Lazily generate a value of an expression
 *  - [[Gen$.fail fail]] - Fail to generate any values of a type
 *  - [[Gen$.recursive recursive]] - A fixed point generator
 *  - [[Gen$.resultOf[T,R0](f* resultOf]] - Generate values with a function or class
 *  - [[Gen$.zip[T1](g1* zip]] - Generate tuples
 */
sealed abstract class Gen[+T] extends Serializable { self =>

  //// Private interface ////

  import Gen.{R, gen}

  // This is no longer used but preserved here for binary compatibility.
  private[scalacheck] def sieveCopy(x: Any): Boolean = true

  // If you implement new Gen[_] directly (instead of using
  // combinators), make sure to use p.initialSeed or p.useInitialSeed
  // in the implementation, instead of using seed directly.
  private[scalacheck] def doApply(p: Gen.Parameters, seed: Seed): R[T]

  //// Public interface ////

  /** A class supporting filtered operations. */
  final class WithFilter(p: T => Boolean) {
    def map[U](f: T => U): Gen[U] = self.suchThat(p).map(f)
    def flatMap[U](f: T => Gen[U]): Gen[U] = self.suchThat(p).flatMap(f)
    def withFilter(q: T => Boolean): WithFilter = self.withFilter(x => p(x) && q(x))
  }

  /** Evaluate this generator with the given parameters */
  def apply(p: Gen.Parameters, seed: Seed): Option[T] =
    doApply(p, seed).retrieve

  def doPureApply(p: Gen.Parameters, seed: Seed, retries: Int = 100): Gen.R[T] = {
    @tailrec def loop(r: Gen.R[T], i: Int): Gen.R[T] =
      if (r.retrieve.isDefined) r
      else if (i > 0) loop(doApply(p, r.seed), i - 1)
      else throw new Gen.RetrievalError()
    loop(doApply(p, seed), retries)
  }

  /**
   * Evaluate this generator with the given parameters.
   *
   * The generator will attempt to generate a valid `T` value. If a
   * valid value is not produced it may retry several times,
   * determined by the `retries` parameter (which defaults to 100).
   *
   * If all the retries fail it will throw a `Gen.RetrievalError`
   * exception.
   */
  def pureApply(p: Gen.Parameters, seed: Seed, retries: Int = 100): T =
    doPureApply(p, seed, retries).retrieve.get

  /** Create a new generator by mapping the result of this generator */
  def map[U](f: T => U): Gen[U] = gen { (p, seed) => doApply(p, seed).map(f) }

  /** Create a new generator by flat-mapping the result of this generator */
  def flatMap[U](f: T => Gen[U]): Gen[U] = gen { (p, seed) =>
    val rt = doApply(p, seed)
    rt.flatMap(t => f(t).doApply(p, rt.seed))
  }

  /** Create a new generator that uses this generator to produce a value
   *  that fulfills the given condition. If the condition is not fulfilled,
   *  the generator fails (returns None). Also, make sure that the provided
   *  test property is side-effect free, e.g. it should not use external vars. */
  def filter(p: T => Boolean): Gen[T] = suchThat(p)

  /** Create a new generator that uses this generator to produce a value
   *  that doesn't fulfill the given condition. If the condition is fulfilled,
   *  the generator fails (returns None). Also, make sure that the provided
   *  test property is side-effect free, e.g. it should not use external vars. */
  def filterNot(p: T => Boolean): Gen[T] = suchThat(x => !p(x))

  /** Creates a non-strict filtered version of this generator. */
  def withFilter(p: T => Boolean): WithFilter = new WithFilter(p)

  /** Create a new generator that uses this generator to produce a value
   *  that fulfills the given condition. If the condition is not fulfilled,
   *  the generator fails (returns None). Also, make sure that the provided
   *  test property is side-effect free, e.g. it should not use external vars.
   *  This method is identical to [Gen.filter]. */
  def suchThat(f: T => Boolean): Gen[T] =
    new Gen[T] {
      def doApply(p: Gen.Parameters, seed: Seed): Gen.R[T] =
        p.useInitialSeed(seed) { (p0, s0) =>
          val r = self.doApply(p0, s0)
          r.copy(r = r.retrieve.filter(f))
        }
    }

  case class RetryUntilException(n: Int) extends RuntimeException(s"retryUntil failed after $n attempts")

  /**
   * Create a generator that calls this generator repeatedly until the
   * given condition is fulfilled. The generated value is then
   * returned. Make sure that the provided test property is
   * side-effect free (it should not use external vars).
   *
   * If the generator fails more than maxTries, a RetryUntilException
   * will be thrown.
   */
  def retryUntil(p: T => Boolean, maxTries: Int): Gen[T] = {
    require(maxTries > 0)
    def loop(params: Gen.Parameters, seed: Seed, tries: Int): R[T] =
      if (tries > maxTries) throw RetryUntilException(tries) else {
        val r = self.doApply(params, seed)
        if (r.retrieve.exists(p)) r else loop(params, r.seed, tries + 1)
      }
    Gen.gen((params, seed) => loop(params, seed, 1))
  }

  /**
   * Create a generator that calls this generator repeatedly until the
   * given condition is fulfilled. The generated value is then
   * returned. Make sure that the provided test property is
   * side-effect free (it should not use external vars).
   *
   *
   * If the generator fails more than 10000 times, a
   * RetryUntilException will be thrown. You can call `retryUntil`
   * with a second parameter to change this number.
   */
  def retryUntil(p: T => Boolean): Gen[T] =
    retryUntil(p, 10000)

  def sample: Option[T] =
    doApply(Gen.Parameters.default, Seed.random()).retrieve

  /** Returns a new property that holds if and only if both this
   *  and the given generator generates the same result, or both
   *  generators generate no result.  */
  def ==[U](g: Gen[U]): Prop = Prop { prms =>
    // test equality using a random seed
    val seed = Seed.random()
    val lhs = doApply(prms, seed).retrieve
    val rhs = g.doApply(prms, seed).retrieve
    if (lhs == rhs) Prop.proved(prms) else Prop.falsified(prms)
  }

  def !=[U](g: Gen[U]): Prop =
    Prop.forAll(this)(r => Prop.forAll(g)(_ != r))

  def !==[U](g: Gen[U]): Prop = Prop { prms =>
    // test inequality using a random seed
    val seed = Seed.random()
    val lhs = doApply(prms, seed).retrieve
    val rhs = g.doApply(prms, seed).retrieve
    if (lhs != rhs) Prop.proved(prms) else Prop.falsified(prms)
  }

  /** Put a label on the generator to make test reports clearer */
  def label(l: String): Gen[T] = new Gen[T] {
    def doApply(p: Gen.Parameters, seed: Seed) =
      p.useInitialSeed(seed) { (p0, s0) =>
        val r = self.doApply(p0, s0)
        r.copy(l = r.labels + l)
      }
  }

  /** Put a label on the generator to make test reports clearer */
  def :|(l: String): Gen[T] = label(l)

  /** Put a label on the generator to make test reports clearer */
  def |:(l: String): Gen[T] = label(l)

  /** Put a label on the generator to make test reports clearer */
  def :|(l: Symbol): Gen[T] = label(l.name)

  /** Put a label on the generator to make test reports clearer */
  def |:(l: Symbol): Gen[T] = label(l.name)

  /** Perform some RNG perturbation before generating */
  def withPerturb(f: Seed => Seed): Gen[T] =
    Gen.gen((p, seed) => doApply(p, f(seed)))
}

object Gen extends GenArities with GenVersionSpecific {

  //// Private interface ////

  import Arbitrary.arbitrary

  /** Just an alias */
  private type P = Parameters

  class RetrievalError extends RuntimeException("couldn't generate value")

  private[scalacheck] trait R[+T] {
    def labels: Set[String] = Set()
    // sieve is no longer used but preserved for binary compatibility
    final def sieve[U >: T]: U => Boolean = (_: U) => true
    protected def result: Option[T]
    def seed: Seed

    def retrieve: Option[T] = result

    def copy[U >: T](
      l: Set[String] = this.labels,
      // s is no longer used but preserved for binary compatibility
      s: U => Boolean = this.sieve,
      r: Option[U] = this.result,
      sd: Seed = this.seed
    ): R[U] = new R[U] {
      override val labels = l
      val seed = sd
      val result = r
    }

    def map[U](f: T => U): R[U] =
      r(retrieve.map(f), seed).copy(l = labels)

    def flatMap[U](f: T => R[U]): R[U] =
      retrieve match {
        case None =>
          r(None, seed).copy(l = labels)
        case Some(t) =>
          val r = f(t)
          r.copy(l = labels | r.labels)
      }
  }

  private[scalacheck] def r[T](r: Option[T], sd: Seed): R[T] = new R[T] {
    val result = r
    val seed = sd
  }

  /** Generator factory method */
  private[scalacheck] def gen[T](f: (P, Seed) => R[T]): Gen[T] = new Gen[T] {
    def doApply(p: P, seed: Seed): R[T] = p.useInitialSeed(seed)(f)
  }

  //// Public interface ////

  /** Generator parameters, used by [[org.scalacheck.Gen.apply]] */
  sealed abstract class Parameters extends Serializable { outer =>

    override def toString: String = {
      val sb = new StringBuilder
      sb.append("Parameters(")
      sb.append(s"size=$size, ")
      sb.append(s"initialSeed=$initialSeed, ")
      sb.append(s"useLegacyShrinking=$useLegacyShrinking)")
      sb.toString
    }

    /**
     * The size of the generated value. Generator implementations are
     * allowed to freely interpret (or ignore) this value. During test
     * execution, the value of this parameter is controlled by
     * [[Test.Parameters.minSize]] and [[Test.Parameters.maxSize]].
     */
    val size: Int

    private[this] def cpy(
      size0: Int = outer.size,
      initialSeed0: Option[Seed] = outer.initialSeed,
      useLegacyShrinking0: Boolean = outer.useLegacyShrinking
    ): Parameters =
      new Parameters {
        val size: Int = size0
        val initialSeed: Option[Seed] = initialSeed0
        override val useLegacyShrinking: Boolean = useLegacyShrinking0
      }

    /**
     * Create a copy of this [[Gen.Parameters]] instance with
     * [[Gen.Parameters.size]] set to the specified value.
     */
    def withSize(size: Int): Parameters =
      cpy(size0 = size)

    /**
     *
     */
    val initialSeed: Option[Seed]

    def withInitialSeed(o: Option[Seed]): Parameters =
      cpy(initialSeed0 = o)

    def withInitialSeed(seed: Seed): Parameters =
      cpy(initialSeed0 = Some(seed))

    def withInitialSeed(n: Long): Parameters =
      cpy(initialSeed0 = Some(Seed(n)))

    def withNoInitialSeed: Parameters =
      cpy(initialSeed0 = None)

    def useInitialSeed[A](seed: Seed)(f: (Parameters, Seed) => A): A =
      initialSeed match {
        case Some(s) => f(this.withNoInitialSeed, s)
        case None => f(this, seed)
      }

    val useLegacyShrinking: Boolean = true

    def disableLegacyShrinking: Parameters =
      withLegacyShrinking(false)

    def enableLegacyShrinking: Parameters =
      withLegacyShrinking(true)

    def withLegacyShrinking(b: Boolean): Parameters =
      cpy(useLegacyShrinking0 = b)

    // no longer used, but preserved for binary compatibility
    @deprecated("cp is deprecated. use cpy.", "1.14.1")
    private case class cp(size: Int = size, initialSeed: Option[Seed] = None) extends Parameters
  }

  /** Provides methods for creating [[org.scalacheck.Gen.Parameters]] values */
  object Parameters {
    /** Default generator parameters instance. */
    val default: Parameters = new Parameters {
      val size: Int = 100
      val initialSeed: Option[Seed] = None
    }
  }

  /** A wrapper type for range types */
  trait Choose[T] extends Serializable {
    /** Creates a generator that returns a value in the given inclusive range */
    def choose(min: T, max: T): Gen[T]
  }

  /** Provides implicit [[org.scalacheck.Gen.Choose]] instances */
  object Choose extends time.JavaTimeChoose {

    class IllegalBoundsError[A](low: A, high: A)
        extends IllegalArgumentException(s"invalid bounds: low=$low, high=$high")

    /**
     * This method gets a ton of use -- so we want it to be as fast as
     * possible for many of our common cases.
     */
    private def chLng(l: Long, h: Long)(p: P, seed: Seed): R[Long] = {
      if (h < l) {
        throw new IllegalBoundsError(l, h)
      } else if (h == l) {
        const(l).doApply(p, seed)
      } else if (l == Long.MinValue && h == Long.MaxValue) {
        val (n, s) = seed.long
        r(Some(n), s)
      } else if (l == Int.MinValue && h == Int.MaxValue) {
        val (n, s) = seed.long
        r(Some(n.toInt.toLong), s)
      } else if (l == Short.MinValue && h == Short.MaxValue) {
        val (n, s) = seed.long
        r(Some(n.toShort.toLong), s)
      } else if (l == 0L && h == Char.MaxValue) {
        val (n, s) = seed.long
        r(Some(n.toChar.toLong), s)
      } else if (l == Byte.MinValue && h == Byte.MaxValue) {
        val (n, s) = seed.long
        r(Some(n.toByte.toLong), s)
      } else {
        val d = h - l + 1
        if (d <= 0) {
          var tpl = seed.long
          var n = tpl._1
          var s = tpl._2
          while (n < l || n > h) {
            tpl = s.long
            n = tpl._1
            s = tpl._2
          }
          r(Some(n), s)
        } else {
          val (n, s) = seed.long
          r(Some(l + (n & 0x7fffffffffffffffL) % d), s)
        }
      }
    }

    private def chDbl(l: Double, h: Double)(p: P, seed: Seed): R[Double] = {
      val d = h - l
      if (d < 0) {
        throw new IllegalBoundsError(l, h)
      } else if (d > Double.MaxValue) {
        val (x, seed2) = seed.long
        if (x < 0) chDbl(l, 0d)(p, seed2) else chDbl(0d, h)(p, seed2)
      } else if (d == 0) {
        r(Some(l), seed)
      } else {
        val (n, s) = seed.double
        r(Some(n * (h-l) + l), s)
      }
    }

    /**
     * Generate a random BigInt within [lower, lower + span).
     *
     * Note that unlike the choose method, whose bounds are inclusive,
     * this method's upper bound is exclusive. We determine how many
     * random bits we need (bitLen), and then round up to the nearest
     * number of bytes (byteLen). We generate the bytes, possibly
     * truncating the most significant byte (bytes(0)) if bitLen is
     * not evenly-divisible by 8.
     *
     * Finally, we check to see if the BigInt we ended up with is in
     * our range. If it is not, we restart this method. The likelihood
     * of needing to restart depends on span. In the worst case we
     * have almost a 50% chance of this (which occurs when span is a
     * power of 2 + 1) and in the best case we never restart (which
     * occurs when span is a power of 2).
     */
    private def chBigInteger(lower: BigInteger, span: BigInteger, seed0: Seed): R[BigInteger] = {
      val bitLen = span.bitLength
      val byteLen = (bitLen + 7) / 8
      val bytes = new Array[Byte](byteLen)
      var seed = seed0
      var i = 0
      while (i < bytes.length) {

        // generate a random long value (i.e. 8 random bytes)
        val (x0, seed1) = seed.long
        var x = x0
        seed = seed1

        // extract each byte in turn and add them to our byte array
        var j = 0
        while (j < 8 && i < bytes.length) {
          val b = (x & 0xff).toByte
          bytes(i) = b
          x = x >>> 8
          i += 1
          j += 1
        }
      }

      // we may not need all 8 bits of our most significant byte. if
      // not, mask off any unneeded upper bits.
      val bitRem = bitLen & 7
      if (bitRem != 0) {
        val mask = 0xff >>> (8 - bitRem)
        bytes(0) = (bytes(0) & mask).toByte
      }

      // construct a BigInteger and see if its valid. if so, we're
      // done. otherwise, we need to restart using our new seed.
      val big = new BigInteger(1, bytes)
      if (big.compareTo(span) < 0) {
        r(Some(big.add(lower)), seed)
      } else {
        chBigInteger(lower, span, seed)
      }
    }

    implicit val chooseLong: Choose[Long] =
      new Choose[Long] {
        def choose(low: Long, high: Long): Gen[Long] =
          if (low > high) throw new IllegalBoundsError(low, high)
          else gen(chLng(low,high))
      }

    implicit val chooseInt: Choose[Int] =
      Choose.xmap[Long, Int](_.toInt, _.toLong)

    implicit val chooseShort: Choose[Short] =
      Choose.xmap[Long, Short](_.toShort, _.toLong)

    implicit val chooseChar: Choose[Char] =
      Choose.xmap[Long, Char](_.toChar, _.toLong)
    implicit val chooseByte: Choose[Byte] =
      Choose.xmap[Long, Byte](_.toByte, _.toLong)

    implicit val chooseDouble: Choose[Double] =
      new Choose[Double] {
        def choose(low: Double, high: Double) =
          if (low > high) throw new IllegalBoundsError(low, high)
          else if (low == Double.NegativeInfinity)
            frequency(1 -> const(Double.NegativeInfinity),
                      9 -> choose(Double.MinValue, high))
          else if (high == Double.PositiveInfinity)
            frequency(1 -> const(Double.PositiveInfinity),
                      9 -> choose(low, Double.MaxValue))
          else gen(chDbl(low,high))
      }

    implicit val chooseFloat: Choose[Float] =
      Choose.xmap[Double, Float](_.toFloat, _.toDouble)

    implicit val chooseFiniteDuration: Choose[FiniteDuration] =
      Choose.xmap[Long, FiniteDuration](Duration.fromNanos, _.toNanos)

    implicit object chooseBigInt extends Choose[BigInt] {
      def choose(low: BigInt, high: BigInt): Gen[BigInt] =
        chooseBigInteger
          .choose(low.bigInteger, high.bigInteger)
          .map(BigInt(_))
    }

    implicit object chooseBigInteger extends Choose[BigInteger] {
      def choose(low: BigInteger, high: BigInteger): Gen[BigInteger] =
        (low.compareTo(high)) match {
          case n if n > 0 => throw new IllegalBoundsError(low, high)
          case 0 => Gen.const(low)
          case _ => /* n < 0 */
            val span = high.subtract(low).add(BigInteger.ONE)
            gen((_, seed) => chBigInteger(low, span, seed))
        }
    }

    /**
     * Choose a BigDecimal number between two given numbers.
     *
     * The minimum scale used will be 34. That means that the
     * fractional part will have at least 34 digits (more if one of
     * the given numbers has a scale larger than 34).
     *
     * The minimum scale was chosen based on Scala's default scale for
     * expanding infinite fractions:
     *
     *   BigDecimal(1) / 3   // 0.3333333333333333333333333333333333
     *
     * See chooseBigDecimalScale for more information about scale.
     */
    implicit val chooseBigDecimal: Choose[BigDecimal] =
      chooseBigDecimalScale(minScale = 34)

    /**
     * The "scale" of a decimal number refers to the number of digits
     * in the fractional part. For example, 3.0000 has a scale of 4.
     *
     * We can generate an arbitrary number of digits in the decimal
     * expansion of a number, so if a user calls choose(0, 1) we need
     * to decide "how much" work to do. The minScale ensures that we
     * do "enough" work to generate interesting numbers.
     *
     * The implicit instance fixes this value, but since users may
     * want to use other scales we expose this method as well.
     */
    private[this] def chooseBigDecimalScale(minScale: Int): Choose[BigDecimal] =
      new Choose[BigDecimal] {
        private val c = chooseJavaBigDecimalScale(minScale)
        def choose(low: BigDecimal, high: BigDecimal): Gen[BigDecimal] =
          c.choose(low.bigDecimal, high.bigDecimal).map(BigDecimal(_))
      }

    /**
     * Choose a java.math.BigDecimal number between two given numbers.
     *
     * See chooseBigDecimal and chooseBigDecimalScale for more comments.
     */
    implicit val chooseJavaBigDecimal: Choose[JavaDecimal] =
      chooseJavaBigDecimalScale(minScale = 34)

    /**
     * See chooseBigDecimalScale for comments.
     */
    private[this] def chooseJavaBigDecimalScale(minScale: Int): Choose[JavaDecimal] =
      new Choose[JavaDecimal] {
        def choose(low: JavaDecimal, high: JavaDecimal): Gen[JavaDecimal] =
        (low.compareTo(high)) match {
          case n if n > 0 => throw new IllegalBoundsError(low, high)
          case 0 => Gen.const(low)
          case _ => /* n < 0 */
            val s = (low.scale max high.scale) max minScale
            val x = if (low.scale < s) low.setScale(s) else low
            val y = if (high.scale < s) high.setScale(s) else high
            chooseBigInteger
              .choose(x.unscaledValue, y.unscaledValue)
              .map(n => new JavaDecimal(n, s))
        }
      }

    /** Transform a Choose[T] to a Choose[U] where T and U are two isomorphic
     *  types whose relationship is described by the provided transformation
     *  functions. (exponential functor map) */
    def xmap[T, U](from: T => U, to: U => T)(implicit c: Choose[T]): Choose[U] =
      new Choose[U] {
        def choose(low: U, high: U): Gen[U] =
          c.choose(to(low), to(high)).map(from)
      }
  }


  //// Various Generator Combinators ////

  /** A generator that always generates the given value */
  implicit def const[T](x: T): Gen[T] = gen((p, seed) => r(Some(x), seed))

  /** A generator that never generates a value */
  def fail[T]: Gen[T] = gen((p, seed) => failed[T](seed))


  /**
   * A fixed point generator. This is useful for making recursive structures
   * e.g.
   *
   * Gen.recursive[List[Int]] { recurse =>
   *   Gen.choose(0, 10).flatMap { idx =>
   *     if (idx < 5) recurse.map(idx :: _)
   *     else Gen.const(idx :: Nil)
   *   }
   * }
   */
  def recursive[A](fn: Gen[A] => Gen[A]): Gen[A] = {
    lazy val result: Gen[A] = lzy(fn(result))
    result
  }

  /** A result that never contains a value */
  private[scalacheck] def failed[T](seed0: Seed): R[T] =
    new R[T] {
      val result: Option[T] = None
      val seed = seed0
    }

  /** A generator that generates a random value in the given (inclusive)
   *  range. If the range is invalid, an IllegalBoundsError exception will be
   *  thrown. */
  def choose[T](min: T, max: T)(implicit c: Choose[T]): Gen[T] =
    c.choose(min, max)

  /** Sequences generators. If any of the given generators fails, the
   *  resulting generator will also fail. */
  def sequence[C,T](gs: Traversable[Gen[T]])(implicit b: Buildable[T, C]): Gen[C] = {
    val g = gen { (p, seed) =>
      gs.foldLeft(r(Some(Vector.empty[T]), seed)) {
        case (rs,g) =>
          val rt = g.doApply(p, rs.seed)
          rt.flatMap(t => rs.map(_ :+ t)).copy(sd = rt.seed)
      }
    }
    g.map(b.fromIterable)
  }

  /** Monadic recursion on Gen
   * This is a stack-safe loop that is the same as:
   *
   * {{{
   *
   * fn(a).flatMap {
   *   case Left(a) => tailRec(a)(fn)
   *   case Right(b) => Gen.const(b)
   *   }
   *
   * }}}
   *
   * which is useful for doing monadic loops without blowing up the
   * stack
   */
  def tailRecM[A, B](a0: A)(fn: A => Gen[Either[A, B]]): Gen[B] = {
    @tailrec
    def tailRecMR(a: A, seed: Seed, labs: Set[String])(fn: (A, Seed) => R[Either[A, B]]): R[B] = {
      val re = fn(a, seed)
      val nextLabs = labs | re.labels
      re.retrieve match {
        case None => r(None, re.seed).copy(l = nextLabs)
        case Some(Right(b)) => r(Some(b), re.seed).copy(l = nextLabs)
        case Some(Left(a)) => tailRecMR(a, re.seed, nextLabs)(fn)
      }
    }

    // This is the "Reader-style" approach to making a stack-safe loop:
    // we put one outer closure around an explicitly tailrec loop
    gen[B] { (p: P, seed: Seed) =>
      tailRecMR(a0, seed, Set.empty) { (a, seed) => fn(a).doApply(p, seed) }
    }
  }

  /** Wraps a generator lazily. The given parameter is only evaluated once,
   *  and not until the wrapper generator is evaluated. */
  def lzy[T](g: => Gen[T]): Gen[T] = {
    lazy val h = g
    gen { (p, seed) => h.doApply(p, seed) }
  }

  /** Wraps a generator for later evaluation. The given parameter is
   *  evaluated each time the wrapper generator is evaluated. */
  def delay[T](g: => Gen[T]): Gen[T] =
    gen { (p, seed) => g.doApply(p, seed) }

  /** Creates a generator that can access its generation parameters */
  def parameterized[T](f: Parameters => Gen[T]): Gen[T] =
    gen { (p, seed) => f(p).doApply(p, seed) }

  /** Creates a generator that can access its generation size */
  def sized[T](f: Int => Gen[T]): Gen[T] =
    gen { (p, seed) => f(p.size).doApply(p, seed) }

  /** A generator that returns the current generation size */
  lazy val size: Gen[Int] = sized { sz => sz }

  /** Creates a resized version of a generator */
  def resize[T](s: Int, g: Gen[T]) = gen((p, seed) => g.doApply(p.withSize(s), seed))

  /** Picks a random value from a list. */
  def oneOf[T](xs: Iterable[T]): Gen[T] =
    if (xs.isEmpty) {
      throw new IllegalArgumentException("oneOf called on empty collection")
    } else {
      val vector = xs.toVector
      choose(0, vector.size - 1).map(vector(_))
    }

  /** Picks a random value from a list.
   *  @todo Remove this overloaded method in the next major release. See #438.
   */
  def oneOf[T](xs: Seq[T]): Gen[T] =
    oneOf(xs: Iterable[T])

  /** Picks a random value from a list */
  def oneOf[T](t0: T, t1: T, tn: T*): Gen[T] = oneOf(t0 +: t1 +: tn)

  /** Picks a random generator from a list */
  def oneOf[T](g0: Gen[T], g1: Gen[T], gn: Gen[T]*): Gen[T] = {
    val gs = g0 +: g1 +: gn
    choose(0, gs.size - 1).flatMap(i => gs(i))
  }

  /** Makes a generator result optional. Either `Some(T)` or `None` will be provided. */
  def option[T](g: Gen[T]): Gen[Option[T]] =
    frequency(1 -> const(None), 9 -> some(g))

  /** A generator that returns `Some(T)` */
  def some[T](g: Gen[T]): Gen[Option[T]] =
    g.map(Some.apply)

  /** Generates a `Left` of `T` or a `Right` of `U` with equal probability. */
  def either[T, U](gt: Gen[T], gu: Gen[U]): Gen[Either[T, U]] =
    oneOf(gt.map(Left(_)), gu.map(Right(_)))

  /** Chooses one of the given generators with a weighted random distribution */
  def frequency[T](gs: (Int, Gen[T])*): Gen[T] = {
    val filtered = gs.iterator.filter(_._1 > 0).toVector
    if (filtered.isEmpty) {
      throw new IllegalArgumentException("no items with positive weights")
    } else {
      var total = 0L
      val builder = TreeMap.newBuilder[Long, Gen[T]]
      filtered.foreach { case (weight, value) =>
        total += weight
        builder += ((total, value))
      }
      val tree = builder.result
      choose(1L, total).flatMap(r => tree.rangeFrom(r).head._2)
    }
  }

  /** Implicit convenience method for using the `frequency` method
   *  like this:
   *  {{{
   *   frequency((1, "foo"), (3, "bar"))
   *  }}}
   */
  implicit def freqTuple[T](t: (Int,T)): (Int,Gen[T]) = (t._1, const(t._2))


  //// List Generators ////

  /** Generates a container of any Traversable type for which there exists an
   *  implicit [[org.scalacheck.util.Buildable]] instance. The elements in the
   *  container will be generated by the given generator. The size of the
   *  generated container is limited by `n`. Depending on what kind of container
   *  that is generated, the resulting container may contain fewer elements than
   *  `n`, but not more. If the given generator fails generating a value, the
   *  complete container generator will also fail. */
  def buildableOfN[C,T](n: Int, g: Gen[T])(implicit
    evb: Buildable[T,C], evt: C => Traversable[T]
  ): Gen[C] = {
    require(n >= 0, s"invalid size given: $n")
    new Gen[C] {
      def doApply(p: P, seed0: Seed): R[C] = {
        var seed: Seed = p.initialSeed.getOrElse(seed0)
        val bldr = evb.builder
        val allowedFailures = Gen.collectionRetries(n)
        var failures = 0
        var count = 0
        while (count < n) {
          val res = g.doApply(p, seed)
          res.retrieve match {
            case Some(t) =>
              bldr += t
              count += 1
            case None =>
              failures += 1
              if (failures >= allowedFailures) return r(None, res.seed)
          }
          seed = res.seed
        }
        r(Some(bldr.result), seed)
      }
    }
  }

  /** Generates a container of any Traversable type for which there exists an
   *  implicit [[org.scalacheck.util.Buildable]] instance. The elements in the
   *  container will be generated by the given generator. The size of the
   *  container is bounded by the size parameter used when generating values. */
  def buildableOf[C,T](g: Gen[T])(implicit
    evb: Buildable[T,C], evt: C => Traversable[T]
  ): Gen[C] =
    sized(s => choose(0, Integer.max(s, 0)))
      .flatMap(n => buildableOfN(n, g)(evb, evt))

  /** Generates a non-empty container of any Traversable type for which there
   *  exists an implicit [[org.scalacheck.util.Buildable]] instance. The
   *  elements in the container will be generated by the given generator. The
   *  size of the container is bounded by the size parameter used when
   *  generating values. */
  def nonEmptyBuildableOf[C,T](g: Gen[T])(implicit
    evb: Buildable[T,C], evt: C => Traversable[T]
  ): Gen[C] =
    sized(s => choose(1, Integer.max(s, 1)))
      .flatMap(n => buildableOfN(n, g)(evb, evt))

  /** A convenience method for calling `buildableOfN[C[T],T](n,g)`. */
  def containerOfN[C[_],T](n: Int, g: Gen[T])(implicit
    evb: Buildable[T,C[T]], evt: C[T] => Traversable[T]
  ): Gen[C[T]] = buildableOfN[C[T], T](n, g)(evb, evt)

  /** A convenience method for calling `buildableOf[C[T],T](g)`. */
  def containerOf[C[_],T](g: Gen[T])(implicit
    evb: Buildable[T,C[T]], evt: C[T] => Traversable[T]
  ): Gen[C[T]] = buildableOf[C[T], T](g)(evb, evt)

  /** A convenience method for calling `nonEmptyBuildableOf[C[T],T](g)`. */
  def nonEmptyContainerOf[C[_],T](g: Gen[T])(implicit
    evb: Buildable[T,C[T]], evt: C[T] => Traversable[T]
  ): Gen[C[T]] = nonEmptyBuildableOf[C[T], T](g)(evb, evt)

  /** Generates a list of random length. The maximum length depends on the
   *  size parameter. This method is equal to calling
   *  `containerOf[List,T](g)`. */
  def listOf[T](g: => Gen[T]) = buildableOf[List[T], T](g)

  /** Generates a non-empty list of random length. The maximum length depends
   *  on the size parameter. This method is equal to calling
   *  `nonEmptyContainerOf[List,T](g)`. */
  def nonEmptyListOf[T](g: => Gen[T]) = nonEmptyBuildableOf[List[T], T](g)

  /** Generates a list with at most the given number of elements. This method
   *  is equal to calling `containerOfN[List,T](n,g)`. */
  def listOfN[T](n: Int, g: Gen[T]) = buildableOfN[List[T], T](n, g)

  /** Generates a map of random length. The maximum length depends on the
   *  size parameter. This method is equal to calling
   *  <code>containerOf[Map,(T,U)](g)</code>. */
  def mapOf[T, U](g: => Gen[(T, U)]) = buildableOf[Map[T, U], (T, U)](g)

  /** Generates a non-empty map of random length. The maximum length depends
   *  on the size parameter. This method is equal to calling
   *  <code>nonEmptyContainerOf[Map,(T,U)](g)</code>. */
  def nonEmptyMap[T,U](g: => Gen[(T,U)]) = nonEmptyBuildableOf[Map[T, U],(T, U)](g)

  /** Generates a map with at most the given number of elements. This method
   *  is equal to calling <code>containerOfN[Map,(T,U)](n,g)</code>. */
  def mapOfN[T,U](n: Int, g: Gen[(T, U)]) = buildableOfN[Map[T, U],(T, U)](n, g)

  /**
   * Generates an infinite stream.
   *
   * Failures in the underlying generator may terminate the stream.
   * Otherwise it will continue forever.
   */
  def infiniteStream[T](g: => Gen[T]): Gen[Stream[T]] = {
    val attemptsPerItem = 10
    def unfold(p: P, seed: Seed, attemptsLeft: Int): Stream[T] =
      if (attemptsLeft <= 0) {
        Stream.empty
      } else {
        val r = g.doPureApply(p, seed)
        r.retrieve match {
          case Some(t) => t #:: unfold(p, r.seed, attemptsPerItem)
          case None => unfold(p, r.seed, attemptsLeft - 1)
        }
      }
    gen { (p, seed0) =>
      val stream = unfold(p, seed0, attemptsPerItem)
      r(Some(stream), seed0.slide)
    }
  }

  /** A generator that picks a random number of elements from a list */
  def someOf[T](l: Iterable[T]) =
    choose(0, l.size).flatMap(pick(_,l))

  /** A generator that picks a random number of elements from a list */
  def someOf[T](g1: Gen[T], g2: Gen[T], gs: Gen[T]*) =
    choose(0, gs.length+2).flatMap(pick(_, g1, g2, gs: _*))

  /** A generator that picks at least one element from a list */
  def atLeastOne[T](l: Iterable[T]) = {
    require(l.size > 0, "There has to be at least one option to choose from")
    choose(1,l.size).flatMap(pick(_,l))
  }

  /** A generator that picks at least one element from a list */
  def atLeastOne[T](g1: Gen[T], g2: Gen[T], gs: Gen[T]*) =
    choose(1, gs.length+2).flatMap(pick(_, g1, g2, gs: _*))

  /** A generator that randomly picks a given number of elements from a list
   *
   * The elements are not guaranteed to be permuted in random order.
   */
  def pick[T](n: Int, l: Iterable[T]): Gen[collection.Seq[T]] = {
    if (n > l.size || n < 0) throw new IllegalArgumentException(s"invalid choice: $n")
    else if (n == 0) Gen.const(Nil)
    else gen { (p, seed0) =>
      val buf = ArrayBuffer.empty[T]
      val it = l.iterator
      var seed = seed0
      var count = 0
      while (it.hasNext) {
        val t = it.next
        count += 1
        if (count <= n) {
          buf += t
        } else {
          val (x, s) = seed.long
          val i = (x & Long.MaxValue % count).toInt
          if (i < n) buf(i) = t
          seed = s
        }
      }
      r(Some(buf), seed)
    }
  }

  /** A generator that randomly picks a given number of elements from a list
   *
   * The elements are not guaranteed to be permuted in random order.
   */
  def pick[T](n: Int, g1: Gen[T], g2: Gen[T], gn: Gen[T]*): Gen[Seq[T]] =
    pick(n, g1 +: g2 +: gn).flatMap(sequence[Seq[T], T](_))

  /** Takes a function and returns a generator that generates arbitrary
   *  results of that function by feeding it with arbitrarily generated input
   *  parameters. */
  def resultOf[T,R0](f: T => R0)(implicit a: Arbitrary[T]): Gen[R0] =
    arbitrary[T].map(f)

  /** Creates a Function0 generator. */
  def function0[A](g: Gen[A]): Gen[() => A] =
    g.map(a => () => a)


  //// Character Generators ////

  private def charSample(cs: Array[Char]): Gen[Char] =
    new Gen[Char] {
      def doApply(p: P, seed0: Seed): Gen.R[Char] = {
        val seed1 = p.initialSeed.getOrElse(seed0)
        val (x, seed2) = seed1.long
        val i = ((x & Long.MaxValue) % cs.length).toInt
        r(Some(cs(i)), seed2)
      }
    }

  /** Generates a numerical character */
  val numChar: Gen[Char] =
    charSample(('0' to '9').toArray)

  /** Generates an upper-case alpha character */
  val alphaUpperChar: Gen[Char] =
    charSample(('A' to 'Z').toArray)

  /** Generates a lower-case alpha character */
  val alphaLowerChar: Gen[Char] =
    charSample(('a' to 'z').toArray)

  /** Generates an alpha character */
  val alphaChar: Gen[Char] =
    charSample((('A' to 'Z') ++ ('a' to 'z')).toArray)

  /** Generates an alphanumerical character */
  val alphaNumChar: Gen[Char] =
    charSample((('0' to '9') ++ ('A' to 'Z') ++ ('a' to 'z')).toArray)

  /** Generates a ASCII character, with extra weighting for printable characters */
  val asciiChar: Gen[Char] =
    charSample((0.toChar to 127.toChar).toArray)

  /** Generates a ASCII printable character */
  val asciiPrintableChar: Gen[Char] =
    charSample((32.toChar to 126.toChar).toArray)

  /** Generates a character that can represent a valid hexadecimal digit. This
    * includes both upper and lower case values.
    */
  val hexChar: Gen[Char] =
    charSample("0123456789abcdef0123456789ABCDEF".toArray)

  //// String Generators ////

  private def mkString(n: Int, sb: StringBuilder, gc: Gen[Char], p: P, seed0: Seed): R[String] = {
    var seed: Seed = seed0
    val allowedFailures = Gen.collectionRetries(n)
    var failures = 0
    var count = 0
    while (count < n) {
      val res = gc.doApply(p, seed)
      res.retrieve match {
        case Some(c) =>
          sb += c
          count += 1
        case None =>
          failures += 1
          if (failures >= allowedFailures) return r(None, res.seed)
      }
      seed = res.seed
    }
    r(Some(sb.toString), seed)
  }

  def stringOfN(n: Int, gc: Gen[Char]): Gen[String] =
    if (n <= 0) Gen.const("")
    else gen { (p, seed) =>
      mkString(n, new StringBuilder(n), gc, p, seed)
    }

  def stringOf(gc: Gen[Char]): Gen[String] =
    gen { (p, seed0) =>
      val (n, seed1) = Gen.mkSize(p, seed0)
      if (n <= 0) r(Some(""), seed1)
      else mkString(n, new StringBuilder(n), gc, p, seed1)
    }

  /** Generates a string that starts with a lower-case alpha character,
   *  and only contains alphanumerical characters */
  val identifier: Gen[String] =
    gen { (p, seed0) =>
      val (n, seed1) = Gen.mkSize(p, seed0)
      val sb = new StringBuilder
      val res1 = alphaLowerChar.doApply(p, seed1)
      sb += res1.retrieve.get
      mkString(n - 1, sb, alphaNumChar, p, res1.seed)
    }

  /** Generates a string of digits */
  val numStr: Gen[String] =
    stringOf(numChar)

  /** Generates a string of upper-case alpha characters */
  val alphaUpperStr: Gen[String] =
    stringOf(alphaUpperChar)

  /** Generates a string of lower-case alpha characters */
  val alphaLowerStr: Gen[String] =
    stringOf(alphaLowerChar)

  /** Generates a string of alpha characters */
  val alphaStr: Gen[String] =
    stringOf(alphaChar)

  /** Generates a string of alphanumerical characters */
  val alphaNumStr: Gen[String] =
    stringOf(alphaNumChar)

  /** Generates a string of ASCII characters, with extra weighting for printable characters */
  val asciiStr: Gen[String] =
    stringOf(asciiChar)

  /** Generates a string of ASCII printable characters */
  val asciiPrintableStr: Gen[String] =
    stringOf(asciiPrintableChar)

  /** Generates a string that can represent a valid hexadecimal digit. This
    * includes both upper and lower case values.
    */
  val hexStr: Gen[String] =
    stringOf(hexChar)

  //// Number Generators ////

  /**
   * Generate a uniformly-distributed Long.
   *
   * This method has an equally likely method of generating every
   * possible Long value.
   */
  val long: Gen[Long] =
    gen { (_, s0) =>
      val (n, s1) = s0.long
      r(Some(n), s1)
    }

  /**
   * Generate a Double uniformly-distributed in [0, 1).
   *
   * This method will generate one of 2^53 distinct Double values in
   * the unit interval.
   */
  val double: Gen[Double] =
    gen { (_, s0) =>
      val (x, s1) = s0.double
      r(Some(x), s1)
    }

  /**
   * Generates a Boolean which has the given chance to be true.
   *
   *  - prob(1.0) is always true
   *  - prob(0.5) is true 50% of the time
   *  - prob(0.1) is true 10% of the time
   *  - prob(0.0) is never true
   */
  def prob(chance: Double): Gen[Boolean] =
    if (chance <= 0.0) Gen.const(false)
    else if (chance >= 1.0) Gen.const(true)
    else gen { (_, s0) =>
      val (x, s1) = s0.double
      r(Some(x < chance), s1)
    }

  /**
   * Generates Double values according to the given gaussian
   * distribution, specified by its mean and standard deviation.
   *
   * Gaussian distributions are also called normal distributions.
   *
   * The range of values is theoretically (-∞, ∞) but 99.7% of all
   * values will be contained within (mean ± 3 * stdDev).
   */
  def gaussian(mean: Double, stdDev: Double): Gen[Double] = {
    def loop(s0: Seed): R[Double] = {
      val (x0, s1) = s0.double
      val (y0, s2) = s1.double
      val x = x0 * 2.0 - 1.0
      val y = y0 * 2.0 - 1.0
      val s = x * x + y * y
      if (s >= 1.0 || s == 0.0) {
        loop(s2)
      } else {
        val scale = stdDev * Math.sqrt(-2.0 * Math.log(s) / s)
        val res = x * scale + mean // dropping y * scale + mean
        r(Some(res), s2)
      }
    }
    gen((_, seed) => loop(seed))
  }

  /**
   * Generates Double values according to the given exponential
   * distribution, specified by its rate parameter.
   *
   * The mean and standard deviation are both equal to 1/rate.
   *
   * The range of values is [0, ∞).
   */
  def exponential(rate: Double): Gen[Double] = {
    require(rate > 0.0, s"rate must be positive (got: $rate)")
    val mean = 1.0 / rate
    gen { (_, s0) =>
      val (x, s1) = s0.double
      r(Some(-Math.log(x) * mean), s1)
    }
  }

  /**
   * Generates Int values according to the given geometric
   * distribution, specified by its mean.
   *
   * This distribution represents the expected number of failures
   * before a successful test, where the probability of a successful
   * test is p = 1 / (mean + 1).
   *
   * The ideal range of values is [0, ∞), although the largest value
   * that can be produced here is 2147483647 (Int.MaxValue).
   */
  def geometric(mean: Double): Gen[Int] = {
    require(mean > 0.0, s"mean must be positive (got: $mean)")
    val p = 1.0 / (mean + 1.0)
    val lognp = Math.log1p(-p) // log(1 - p)
    gen { (_, s0) =>
      val (u, s1) = s0.double
      r(Some(Math.floor(Math.log(u) / lognp).toInt), s1)
    }
  }

  /**
   * Generates Int values according to the given Poisson distribution,
   * specified by its rate parameters.
   *
   * The mean equals the rate; the standard deviation is sqrt(rate).
   *
   * In principle any positive value is a valid rate parameter.
   * However, our method of generating values cannot handle large
   * rates, so we require rate <= 745.
   */
  def poisson(rate: Double): Gen[Int] = {
    require(0 < rate && rate <= 745.0, s"rate must be between 0 and 745 (got $rate)")
    val L = Math.exp(-rate)
    def loop(s0: Seed, k: Int, p: Double): R[Int] =
      if (p <= L) {
        r(Some(k - 1), s0)
      } else {
        val (x, s1) = s0.double
        loop(s1, k + 1, p * x)
      }

    gen((_, s) => loop(s, 0, 1.0))
  }

  /**
   * Generates Int values according to the given binomial
   * distribution, specified by the number of trials to conduct, and
   * the probability of a true test.
   *
   * This distribution counts the number of trials which were
   * successful according to a given test probability.
   *
   * The range of values is [0, trials].
   */
  def binomial(test: Gen[Boolean], trials: Int): Gen[Int] = {
    def loop(ps: Gen.Parameters, s: Seed, i: Int, n: Int): R[Int] =
      if (i >= trials) {
        r(Some(n), s)
      } else {
        val r = test.doPureApply(ps, s)
        val success = r.retrieve.get
        loop(ps, r.seed, i + 1, if (success) n + 1 else n)
      }
    gen((ps, s) => loop(ps, s, 0, 0))
  }


  /** Generates positive numbers of uniform distribution, with an
   *  upper bound of the generation size parameter. */
  def posNum[T](implicit num: Numeric[T], c: Choose[T]): Gen[T] = {
    import num._
    num match {
      case _: Fractional[_] => sized(n => c.choose(zero, max(fromInt(n), one)).suchThat(_ != zero))
      case _ => sized(n => c.choose(one, max(fromInt(n), one)))
    }
  }

  /** Generates negative numbers of uniform distribution, with an
   *  lower bound of the negated generation size parameter. */
  def negNum[T](implicit num: Numeric[T], c: Choose[T]): Gen[T] = posNum.map(num.negate(_))

  /** Generates numbers within the given inclusive range, with
   *  extra weight on zero, +/- unity, both extremities, and any special
   *  numbers provided. The special numbers must lie within the given range,
   *  otherwise they won't be included. */
  def chooseNum[T](minT: T, maxT: T, specials: T*)(
    implicit num: Numeric[T], c: Choose[T]
  ): Gen[T] = {
    import num._
    val basics = List(minT, maxT, zero, one, -one)
    val basicsAndSpecials = for {
      t <- specials ++ basics if t >= minT && t <= maxT
    } yield (1, const(t))
    val other = (basicsAndSpecials.length, c.choose(minT, maxT))
    val allGens = basicsAndSpecials :+ other
    frequency(allGens: _*)
  }


  //// Misc Generators ////

  /** Generates a version 4 (random) UUID. */
  lazy val uuid: Gen[UUID] = for {  // FIXME: Remove lazy
    l1 <- Gen.choose(Long.MinValue, Long.MaxValue)
    l2 <- Gen.choose(Long.MinValue, Long.MaxValue)
    y <- Gen.oneOf('8', '9', 'a', 'b')
  } yield UUID.fromString(
    new UUID(l1,l2).toString.updated(14, '4').updated(19, y)
  )

  lazy val calendar: Gen[Calendar] = { // FIXME: Remove lazy
    import Calendar._

    def adjust(c: Calendar)(f: Calendar => Unit): Calendar = { f(c); c }

    // We want to be sure we always initialize the calendar's time. By
    // default, Calendar.getInstance uses the system time. We always
    // overwrite it with a deterministically-generated time to be sure
    // that calendar generation is also deterministic.
    //
    // We limit the time (in milliseconds) because extreme values will
    // cause Calendar.getTime calls to fail. This range is relatively
    // large but safe:
    //
    //   -62135751600000 is 1 CE
    //    64087186649116 is 4000 CE
    val calendar: Gen[Calendar] =
      Gen.chooseNum(-62135751600000L, 64087186649116L).map { t =>
        adjust(Calendar.getInstance)(_.setTimeInMillis(t))
      }

    def yearGen(c: Calendar): Gen[Int] =
      Gen.chooseNum(c.getGreatestMinimum(YEAR), c.getLeastMaximum(YEAR))

    def moveToNearestLeapDate(c: Calendar, year: Int): Calendar = {
      @tailrec def loop(y: Int): Calendar = {
        c.set(YEAR, y)
        if (c.getActualMaximum(DAY_OF_YEAR) > 365) c else loop(y + 1)
      }
      loop(if (year + 4 > c.getLeastMaximum(YEAR)) year - 5 else year)
    }

    val beginningOfDayGen: Gen[Calendar] =
      calendar.map(c => adjust(c) { c =>
        c.set(HOUR_OF_DAY, 0)
        c.set(MINUTE, 0)
        c.set(SECOND, 0)
        c.set(MILLISECOND, 0)
      })

    val endOfDayGen: Gen[Calendar] =
      calendar.map(c => adjust(c) { c =>
        c.set(HOUR_OF_DAY, 23)
        c.set(MINUTE, 59)
        c.set(SECOND, 59)
        c.set(MILLISECOND, 59)
      })

    val firstDayOfYearGen: Gen[Calendar] =
      for { c <- calendar; y <- yearGen(c) } yield adjust(c)(_.set(y, JANUARY, 1))

    val lastDayOfYearGen: Gen[Calendar] =
      for { c <- calendar; y <- yearGen(c) } yield adjust(c)(_.set(y, DECEMBER, 31))

    val closestLeapDateGen: Gen[Calendar] =
      for { c <- calendar; y <- yearGen(c) } yield moveToNearestLeapDate(c, y)

    val lastDayOfMonthGen: Gen[Calendar] =
      calendar.map(c => adjust(c)(_.set(DAY_OF_MONTH, c.getActualMaximum(DAY_OF_MONTH))))

    val firstDayOfMonthGen: Gen[Calendar] =
      calendar.map(c => adjust(c)(_.set(DAY_OF_MONTH, 1)))

    Gen.frequency(
      (1, firstDayOfYearGen),
      (1, lastDayOfYearGen),
      (1, closestLeapDateGen),
      (1, beginningOfDayGen),
      (1, endOfDayGen),
      (1, firstDayOfMonthGen),
      (1, lastDayOfMonthGen),
      (7, calendar))
  }

  val finiteDuration: Gen[FiniteDuration] =
    // Duration.fromNanos doesn't allow Long.MinValue since it would create a
    // duration that cannot be negated.
    chooseNum(Long.MinValue + 1, Long.MaxValue).map(Duration.fromNanos)

  /**
   * Generates instance of Duration.
   *
   * In addition to `FiniteDuration` values, this can generate `Duration.Inf`,
   * `Duration.MinusInf`, and `Duration.Undefined`.
   */
  val duration: Gen[Duration] = frequency(
    1 -> const(Duration.Inf),
    1 -> const(Duration.MinusInf),
    1 -> const(Duration.Undefined),
    1 -> const(Duration.Zero),
    6 -> finiteDuration)

  // used to compute a uniformly-distributed size
  private def mkSize(p: Gen.Parameters, seed0: Seed): (Int, Seed) = {
    val maxSize = Integer.max(p.size + 1, 1)
    val (x, seed1) = seed0.long
    (((x & Long.MaxValue) % maxSize).toInt, seed1)
  }

  // used to calculate how many per-item retries we should allow.
  private def collectionRetries(n: Int): Int =
    Integer.max(10, n / 10)
}
