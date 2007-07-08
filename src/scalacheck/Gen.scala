package scalacheck

import Prop._

trait RandomGenerator {
  def choose(inclusiveRange: (Int,Int)): Int
}

object StdRand extends RandomGenerator {
  import scala.Math._
  private val r = new java.util.Random
  def choose(range: (Int,Int)) = range match {
    case (l,h) if(l == h) => l
    case (l,h) if(h < l)  => h
    case (l,h)            => l + r.nextInt((h-l)+1)
  }
}

/** Dummy type that represents types that supports the arbitrary function.
 *  This could have been done more like a Haskell type class, with arbitrary as
 *  a member of Arbitrary, but placing the arbitrary function in the Gen object
 *  and adding implicit functions that converts an Arbitrary[T] to a Gen[T],
 *  helps Scala's type inference.
 *  To make your own "instance" of the Arbitrary class for a type U, define an
 *  implicit function that takes a value of type Arbitrary[U] and returns a
 *  value of type Gen[U]. The Arbitrary[U] value has no meaning in itself,
 *  its just there to make it a usable implicit function.
 */
sealed class Arbitrary[T] {}

/** Record that encapsulates all parameters required for data generation */
case class GenPrms(size: Int, rand: RandomGenerator) {
  def resize(newSize: Int) = GenPrms(newSize,rand)
}

/** Class that represents a generator. You shouldn't (and couldn't) make
 *  instances or subclasses of this class directly. To create custom
 *  generators, the combinators in the Gen object should be used.
 */
abstract sealed class Gen[+T](g: GenPrms => Option[T]) {

  def apply(prms: GenPrms) = g(prms)

  def map[U](f: T => U): Gen[U] = Gen.mkGen(prms => for {
    t <- this(prms)
  } yield f(t))

  def flatMap[U](f: T => Gen[U]): Gen[U] = Gen.mkGen(prms => for {
    t <- this(prms)
    u <- f(t)(prms)
  } yield u)

  def filter(p: T => Boolean): Gen[T] = Gen.mkGen(prms => for {
    t <- this(prms)
    u <- if (p(t)) Some(t) else None
  } yield u)

  def suchThat(p: T => Boolean): Gen[T] = filter(p)

}

/** Contains combinators for building generators, and has implicit functions
 *  for generating arbitrary values of common types.
 */
object Gen extends Testable {

  // Internal support functions

  private def mkGen[T](g: GenPrms => Option[T]): Gen[T] = new Gen(g) {}

  private def sequence[T](l: List[Gen[T]]): Gen[List[T]] = {
    def consGen(gt: Gen[T], gts: Gen[List[T]]) = for {
      t  <- gt
      ts <- gts
    } yield t::ts

    l.foldRight(value[List[T]](Nil))(consGen _)
  }


  // Generator combinators

  /** Generates an arbitrary value of type T. It should be used as Gen[T],
   *  so there must exist an implicit function that can convert Arbitrary[T]
   *  into Gen[T].
   */
  def arbitrary[T]: Arbitrary[T] = new Arbitrary[T]


  addProperty("Gen.value", (x: Int, sz: Int) => 
    value(x)(GenPrms(sz,StdRand)).get == x
  )

  /** A generator that always generates a given value */
  def value[T](x: T) = mkGen(p => Some(x))

  
  addProperty("Gen.fail", (x: Int, sz: Int) => 
    fail(GenPrms(sz,StdRand)) == None
  )

  /** A generator that never generates a value */
  def fail[T]: Gen[T] = mkGen(p => None)


  addProperty("Gen.choose", (l: Int, h: Int, sz: Int) => {
    val x = choose(l,h)(GenPrms(sz,StdRand)).get
    h >= l ==> (x >= l && x <= h)
  })

  /** A generator that generates a random integer in the given (inclusive)
   *  range.
   */
  def choose(inclusiveRange: (Int,Int)) =
    parameterized(prms => value(prms.rand.choose(inclusiveRange)))


  /** Creates a generator that can access its generation parameters
   */
  def parameterized[T](f: GenPrms => Gen[T]): Gen[T] =
    mkGen(prms => f(prms)(prms))


  /** Creates a generator that can access its generation size
   */
  def sized[T](f: Int => Gen[T]) = parameterized(prms => f(prms.size))


  /** Creates a resized version of a generator
   */
  def resize[T](s: Int, g: Gen[T]) = mkGen(prms => g(prms.resize(s)))


  addProperty("Gen.elements", (l: List[Int], sz: Int) => 
    elements(l)(GenPrms(sz,StdRand)) match {
      case None => l.isEmpty
      case Some(n) => l.contains(n)
    }
  )

  /** A generator that returns a random element from a list
   */
  def elements[T](xs: Seq[T]) = if(xs.isEmpty) fail else for {
    i <- choose((0,xs.length-1))
  } yield xs(i)


  /** Picks a random generator from a list
   */
  def oneOf[T](gs: Seq[Gen[T]]) = if(gs.isEmpty) fail else for {
    i <- choose((0,gs.length-1))
    x <- gs(i)
  } yield x


  /** Generates a list of random length. The maximum length depends on the
   *  size parameter
   */
  def listOf[T](g: Gen[T]) = arbitraryList(null)(a => g)


  /** Generates a non-empty list of random length. The maximum length depends
   *  on the size parameter
   */
  def listOf1[T](g: Gen[T]) = for {
    x  <- g
    xs <- arbitraryList(null)(a => g)
  } yield x::xs


  addProperty("Gen.vectorOf", (len: Int, sz: Int) => 
    () imply {
      case () if len == 0 =>
        vectorOf(len,fail)(GenPrms(sz,StdRand)).get.length == 0 &&
        vectorOf(len,value(0))(GenPrms(sz,StdRand)).get.length == 0
      case () if len > 0 =>
        vectorOf(len,fail)(GenPrms(sz,StdRand)) == None &&
        vectorOf(len,value(0))(GenPrms(sz,StdRand)).get.length == len
    }
  )

  /** Generates a list of the given length
   */
  def vectorOf[T](n: Int, g: Gen[T]): Gen[List[T]] = sequence(List.make(n,g))


  // Implicit generators for common types

  implicit def arbitraryBool(x: Arbitrary[Boolean]): Gen[Boolean] =
    elements(List(true,false))

  /** Generates an arbitrary integer */
  implicit def arbitraryInt(x: Arbitrary[Int]) = sized (s => choose((-s,s)))

  /** Generates an arbitrary char */
  implicit def arbitraryChar(x: Arbitrary[Char]): Gen[Char] =
    for {n <- choose((0,255))} yield n.toChar

  /** Generates an arbitrary string */
  implicit def arbitraryString(x: Arbitrary[String]): Gen[String] = for {
    cs <- listOf(arbitrary[Char])
  } yield List.toString(cs)

  /** Generates a list of arbitrary elements. The maximum length of the list
   *  depends on the size parameter.
   */
  implicit def arbitraryList[T](x: Arbitrary[List[T]])
    (implicit f: Arbitrary[T] => Gen[T]): Gen[List[T]] = sized(size => for
  {
    n <- choose(0,size)
    l <- sequence(List.make(n, f(arbitrary)))
  } yield l )

  implicit def arbitraryTuple2[T1,T2](x: Arbitrary[Tuple2[T1,T2]])
    (implicit
      f1: Arbitrary[T1] => Gen[T1],
      f2: Arbitrary[T2] => Gen[T2]
    ): Gen[Tuple2[T1,T2]] = for
  {
    t1 <- f1(arbitrary)
    t2 <- f2(arbitrary)
  } yield (t1,t2)

  implicit def arbitraryTuple3[T1,T2,T3](x: Arbitrary[Tuple3[T1,T2,T3]])
    (implicit
      f1: Arbitrary[T1] => Gen[T1],
      f2: Arbitrary[T2] => Gen[T2],
      f3: Arbitrary[T3] => Gen[T3]
    ): Gen[Tuple3[T1,T2,T3]] = for
  {
    t1 <- f1(arbitrary)
    t2 <- f2(arbitrary)
    t3 <- f3(arbitrary)
  } yield (t1,t2,t3)

  implicit def arbitraryTuple4[T1,T2,T3,T4](x: Arbitrary[Tuple4[T1,T2,T3,T4]])
    (implicit
      f1: Arbitrary[T1] => Gen[T1],
      f2: Arbitrary[T2] => Gen[T2],
      f3: Arbitrary[T3] => Gen[T3],
      f4: Arbitrary[T4] => Gen[T4]
    ): Gen[Tuple4[T1,T2,T3,T4]] = for
  {
    t1 <- f1(arbitrary)
    t2 <- f2(arbitrary)
    t3 <- f3(arbitrary)
    t4 <- f4(arbitrary)
  } yield (t1,t2,t3,t4)

}
