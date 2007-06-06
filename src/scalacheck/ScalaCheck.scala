package scalacheck


// Generation //////////////////////////////////////////////////////////////////

trait RandomGenerator {
  def choose(inclusiveRange: (Int,Int)): Int
}

object StdRand extends RandomGenerator {
  import scala.Math._
  private val r = new java.util.Random
  def choose(range: (Int,Int)) = range match {
    case (l,h) if(l <  0 && h <  0) => -(r.nextInt(abs(l) + 1) + abs(h))
    case (l,h) if(l <  0 && h >= 0) => r.nextInt(abs(l) + h + 1) + l
    case (l,h) if(l >= 0 && h >= 0) => r.nextInt(h + 1) + l
    case (l,h)                      => l
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
object Gen {

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

  /** A generator that always generates a given value */
  def value[T](x: T) = mkGen(p => Some(x))

  /** A generator that never generates a value */
  def fail[T]: Gen[T] = mkGen(p => None)

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

  /** A generator that returns a random element from a list
   */
  def elements[T](xs: Seq[T]) = for {
    i <- choose((0,xs.length-1))
  } yield xs(i)

  /** Picks a random generator from a list
   */
  def oneOf[T](gs: Seq[Gen[T]]) = for {
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


// Properties //////////////////////////////////////////////////////////////////

object Prop {

  import Gen.{arbitrary, value, fail}

  type Prop = Gen[PropRes]

  abstract sealed class PropRes(val args: List[String])

  case class PropTrue(as: List[String]) extends PropRes(as)
  case class PropFalse(as: List[String]) extends PropRes(as)
  case class PropException(e: Throwable, as: List[String]) extends PropRes(as)

  // Private support functions

  private def mkProp(p: => Prop, as: Any*): Prop = for {
    r1 <- try { p } catch { case e => value(PropException(e,Nil)) }
    ss = as.map(_.toString).toList
    r2 <- r1 match {
            case PropTrue(ss2)        => value(PropTrue(ss ::: ss2))
            case PropFalse(ss2)       => value(PropFalse(ss ::: ss2))
            case PropException(e,ss2) => value(PropException(e,ss ::: ss2))
          }
  } yield r2


  // Property combinators

  def rejected = fail

  def ==> (b: Boolean, p: => Prop): Prop = if (b) p else rejected

  def forAll[T](g: Gen[T])(f: T => Prop): Prop = for {
    t <- g
    r <- mkProp(f(t), t)
  } yield r


  // Convenience functions

  implicit def extBoolean(b: Boolean) = new ExtBoolean(b)
  class ExtBoolean(b: Boolean) {
    def ==>(t: => Prop) = if(b) t else fail
  }


  // Implicit properties for common types

  implicit def propBoolean(b: Boolean) =
    value(if(b) PropTrue(Nil) else PropFalse(Nil))

  def property[A1,P]
    (f:  Function1[A1,P])(implicit
     p:  P => Prop,
     g1: Arbitrary[A1] => Gen[A1]) = for
  {
    a1 <- g1(arbitrary)
    r  <- mkProp(p(f(a1)), a1)
  } yield r

  def property[A1,A2,P]
    (f:  Function2[A1,A2,P])(implicit
     p:  P => Prop,
     g1: Arbitrary[A1] => Gen[A1],
     g2: Arbitrary[A2] => Gen[A2]) = for
  {
    a1 <- g1(arbitrary)
    a2 <- g2(arbitrary)
    r  <- mkProp(p(f(a1,a2)),a1,a2)
  } yield r

  def property[A1,A2,A3,P]
    (f:  Function3[A1,A2,A3,P])(implicit
     p:  P => Prop,
     g1: Arbitrary[A1] => Gen[A1],
     g2: Arbitrary[A2] => Gen[A2],
     g3: Arbitrary[A3] => Gen[A3]) = for
  {
    a1 <- g1(arbitrary)
    a2 <- g2(arbitrary)
    a3 <- g3(arbitrary)
    r  <- mkProp(p(f(a1,a2,a3)),a1,a2,a3)
  } yield r

  def property[A1,A2,A3,A4,P]
    (f:  Function4[A1,A2,A3,A4,P])(implicit
     p:  P => Prop,
     g1: Arbitrary[A1] => Gen[A1],
     g2: Arbitrary[A2] => Gen[A2],
     g3: Arbitrary[A2] => Gen[A3],
     g4: Arbitrary[A3] => Gen[A4]) = for
  {
    a1 <- g1(arbitrary)
    a2 <- g2(arbitrary)
    a3 <- g3(arbitrary)
    a4 <- g4(arbitrary)
    r  <- mkProp(p(f(a1,a2,a3,a4)),a1,a2,a3,a4)
  } yield r

}


// Testing /////////////////////////////////////////////////////////////////////

/** Test parameters */
case class TestPrms(minSuccessfulTests: Int, maxDiscardedTests: Int,
  maxSize: Int)

/** Test statistics */
case class TestStats(result: TestResult, succeeded: Int, discarded: Int)

abstract sealed class TestResult

/** The property test passed
 */
case class TestPassed extends TestResult

/** The property was proved wrong with the given concrete arguments.
 */
case class TestFailed(args: List[String]) extends TestResult

/** The property test was exhausted, it wasn't possible to generate enough
 *  concrete arguments satisfying the preconditions to get enough passing
 *  property evaluations.
 */
case class TestExhausted extends TestResult

/** An exception was raised when trying to evaluate the property with the
 *  given concrete arguments.
 */
case class TestPropException(e: Throwable,args: List[String]) extends TestResult

/** An exception was raised when trying to generate concrete arguments
 *  for evaluating the property.
 */
case class TestGenException(e: Throwable) extends TestResult

object Test {

  import Prop._

  // Testing functions

  val defaultTestPrms = TestPrms(100,50000,100)

  type TestInspector = (Option[PropRes],Int,Int) => Unit

  /** Tests a property with the given testing parameters, and returns
   *  the test results.
   */
  def check(prms: TestPrms, p: Prop): TestStats = check(prms,p, (r,s,d) => ())

  /** Tests a property with the given testing parameters, and returns
   *  the test results. <code>f</code> is a function which is called each
   *  time the property is evaluted.
   */
  def check(prms: TestPrms, p: Prop, f: TestInspector): TestStats =
  {
    abstract sealed class Either[+T,+U]
    case class Left[+T,+U](l: T) extends Either[T,U]
    case class Right[+T,+U](r: U) extends Either[T,U]

    var nd = 0
    var ns = 0
    var tr: TestResult = null

    while(tr == null)
    {
      val size = (ns * prms.maxSize) / prms.minSuccessfulTests + nd / 10
      val genprms = GenPrms(size, StdRand)
      (try { Right(p(genprms)) } catch { case e => Left(e) }) match {
        case Left(e)   => tr = TestGenException(e)
        case Right(pr) =>
          pr match {
            case None =>
              nd = nd + 1
              if(nd >= prms.maxDiscardedTests) tr = TestExhausted
            case Some(PropTrue(_)) =>
              ns = ns + 1
              if(ns >= prms.minSuccessfulTests) tr = TestPassed
            case Some(PropFalse(as)) => tr = TestFailed(as)
            case Some(PropException(e,as)) => tr = TestPropException(e,as)
          }
          f(pr,ns,nd)
      }
    }

    TestStats(tr, ns, nd)
  }

  /** Tests a property and prints results to the console
   */
  def check(p: Prop): TestStats =
  {
    def printTmp(res: Option[PropRes], succeeded: Int, discarded: Int) = {
      if(discarded > 0)
        Console.printf("\rPassed {0} tests; {1} discarded",succeeded,discarded)
      else Console.printf("\rPassed {0} tests",succeeded)
      Console.flush
    }

    val tr = check(defaultTestPrms,p,printTmp)

    tr.result match {
      case TestGenException(e) =>
        Console.printf("\r*** Exception raised when generating arguments:\n{0}\n", e)
      case TestPropException(e,args) =>
        Console.printf("\r*** Exception raised when evaluating property\n")
        Console.printf("The arguments that caused the exception was:\n{0}\n\n", args)
        Console.printf("The raised exception was:\n{0}\n", e)
      case TestFailed(args) =>
        Console.printf("\r*** Failed, after {0} tests:                  \n", tr.succeeded)
        Console.printf("The arguments that caused the failure was:\n{0}\n\n", args)
      case TestExhausted() =>
        Console.printf(
          "\r*** Gave up, after only {1} passed tests. {0} tests were discarded.\n",
          tr.discarded, tr.succeeded)
      case TestPassed() =>
        Console.printf("\r+++ OK, passed {0} tests.                    \n", tr.succeeded)
    }

    tr
  }

}
