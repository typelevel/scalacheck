package squickcheck


// Generation //////////////////////////////////////////////////////////////////

trait RandomGenerator {
  def choose(inclusiveRange: (Int,Int)): Int
}

object StdRand extends RandomGenerator {
  private val r = new java.util.Random
  def choose(range: (Int,Int)) = range match {
    case (low,high) => r.nextInt(low + high + 1) + low
  }
}

/** Record that encapsulates all parameters required for data generation */
case class GenPrms(size: Int, rand: RandomGenerator) {
  def resize(newSize: Int) = GenPrms(newSize,rand)
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

/** Class that represents a generator. You shouldn't (and couldn't) make
 *  instances or subclasses of this class directly. To create custom
 *  generators, the combinators in the Gen object should be used.
 */
abstract sealed class Gen[+T](g: GenPrms => Option[T]) {

  def get(prms: GenPrms) = g(prms)

  def map[U](f: T => U): Gen[U] = Gen.mkGen(prms => for {
    t <- get(prms)
  } yield f(t))

  def flatMap[U](f: T => Gen[U]): Gen[U] = Gen.mkGen(prms => for {
    t <- get(prms)
    u <- f(t).get(prms)
  } yield u)

  def filter(p: T => Boolean): Gen[T] = Gen.mkGen(prms => for {
    t <- get(prms)
    u <- if (p(t)) Some(t) else None
  } yield u)

  def suchThat(p: T => Boolean): Gen[T] = filter(p)

}

/** Contains combinators for building generators, and has implicit functions
 *  for generating arbitrary values of common types.
 */
object Gen {

  // Internal support functions

  private def mkGen[T](g: GenPrms => Option[T]): Gen[T] = new Gen(g) {}


  // Generator combinators

  /** Generates an arbitrary value of type T. It should be used as Gen[T],
   *  so there must exist an implicit function that can convert Arbitrary[T]
   *  into Gen[T].
   */
  def arbitrary[T]: Arbitrary[T] = new Arbitrary[T]

  /** A generator that always generates a given value */
  def value[T](x: T) = mkGen(p => Some(x))

  /** A generator that never generates a value */
  def fail[T]: Gen[T] = mkGen(p => None)

  /** A generator that generates a random integer in the given (inclusive)
   *  range.
   */
  def choose(inclusiveRange: (Int,Int)) =
    parameterized(prms => value(prms.rand.choose(inclusiveRange)))

  def parameterized[T](f: GenPrms => Gen[T]): Gen[T] =
    mkGen(prms => f(prms).get(prms))

  def sized[T](f: Int => Gen[T]) = parameterized(prms => f(prms.size))

  def resize[T](s: Int, g: Gen[T]) = mkGen(prms => g.get(prms.resize(s)))

  def elements[T](xs: Seq[T]) = for {
    i <- choose((0,xs.length-1))
  } yield xs(i)

  def oneof[T](gs: Seq[Gen[T]]) = for {
    i <- choose((0,gs.length-1))
    x <- gs(i)
  } yield x


  // Implicit generators for common types

  implicit def arbitraryInt(x: Arbitrary[Int]) = sized (s => choose((0,s)))

  implicit def arbitraryList[T](x: Arbitrary[List[T]])
    (implicit f: Arbitrary[T] => Gen[T]): Gen[List[T]] =
  {
    def g(gt: Gen[T], gts: Gen[List[T]]): Gen[List[T]] = for {
      t  <- gt
      ts <- gts
    } yield t::ts

    val e: Gen[List[T]] = value(Nil)

    sized(s => List.make(s, ()).map(x => f(arbitrary)).foldRight(e)(g _))
  }

}



// Testing /////////////////////////////////////////////////////////////////////

/** A result from a single test */
case class Result(ok: Boolean, args: List[String])

/** Represents something that can be tested */
case class Testable(prop: Gen[Result]) {
  def apply(prms: GenPrms): Option[Result] = prop.get(prms)
}

object Test {

  import Gen.{arbitrary, value, fail}


  // Private support functions

  private def mkRes(r: Result, as: Any*) =
    Result(r.ok, as.map(_.toString).toList ::: r.args)


  // Testing functions

  def quickCheck(t: Testable) = {
    val prms = GenPrms(10, StdRand)
    t(prms) match {
      case None => Console.println("Test UNDECIDABLE")
      case Some(r) => if (r.ok) Console.println("Test SUCCESS")
        else {
          Console.println("Test FAILURE")
          Console.println(r.args)
        }
    }
  }


  // Convenience functions

  implicit def extBoolean(b: Boolean) = new ExtBoolean(b)
  class ExtBoolean(b: Boolean) {
    def ==>(t: Testable) = Test.==>(b,t)
  }


  // Testables

  def ==> (p: Boolean, t: Testable): Testable = if (p) t else rejected

  def rejected = Testable(fail)

  def testable[A1]
    (f:  Function1[A1,Testable])(implicit
     g1: Arbitrary[A1] => Gen[A1]) = Testable(for
  {
    a1 <- g1(arbitrary)
    r  <- f(a1).prop
  } yield mkRes(r, a1))

  def testable[A1,A2]
    (f:  Function2[A1,A2,Testable])(implicit
     g1: Arbitrary[A1] => Gen[A1],
     g2: Arbitrary[A2] => Gen[A2]) = Testable(for
  {
    a1 <- g1(arbitrary)
    a2 <- g2(arbitrary)
    r  <- f(a1,a2).prop
  } yield mkRes(r, a1, a2))

  def testable[A1,A2,A3]
    (f:  Function3[A1,A2,A3,Testable])(implicit
     g1: Arbitrary[A1] => Gen[A1],
     g2: Arbitrary[A2] => Gen[A2],
     g3: Arbitrary[A3] => Gen[A3]) = Testable(for
  {
    a1 <- g1(arbitrary)
    a2 <- g2(arbitrary)
    a3 <- g3(arbitrary)
    r  <- f(a1,a2,a3).prop
  } yield mkRes(r, a1, a2, a3))

  implicit def testable(b: Boolean) = Testable(value(Result(b,Nil)))

}


// Usage ///////////////////////////////////////////////////////////////////////

object TestIt extends Application {

  import Gen._
  import Test._

  val n = arbitrary[Int]

  val l = arbitrary[List[Int]]

  val x = for {
    x <- elements(List(1,6,8)) suchThat (_ < 6)
    y <- arbitrary[Int]
  } yield (x, y)

  val prms = GenPrms(100, StdRand)

  Console.println(n.get(prms))
  Console.println(l.get(prms))
  Console.println(x.get(prms))


  val pf1 = (n:Int) => (n == 3) ==> (n > 3)

  quickCheck(testable(pf1))

  val pf2: (List[Int], Int) => Testable = (n,m) => n.length == m

  quickCheck(testable(pf2))
}
