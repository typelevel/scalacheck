package scalacheck

import scala.collection.mutable.ListBuffer

/** Class that represents a generator. You shouldn't make
 *  instances of this class directly. To create custom
 *  generators, the combinators in the Gen object should be used.
 */
abstract class Gen[+T] {

  def apply(prms: Gen.Params): Option[T]

  def map[U](f: T => U): Gen[U] = Gen.mkGen(prms => this(prms).map(f))

  def flatMap[U](f: T => Gen[U]): Gen[U] = Gen.mkGen(prms => for {
    t <- this(prms)
    u <- f(t)(prms)
  } yield u)

  def filter(p: T => Boolean): Gen[T] = Gen.mkGen(prms => for {
    t <- this(prms)
    u <- if (p(t)) Some(t) else None
  } yield u)

  def suchThat(p: T => Boolean): Gen[T] = filter(p)

  def combine[U,V](g: Gen[U])(f: (Option[T],Option[U]) => Option[V]): Gen[V] =
    Gen.mkGen(prms => f(this(prms), g(prms)))

}


/** Contains combinators for building generators. */
object Gen extends Testable {

  import Prop._

  // Types

  /** Record that encapsulates all parameters required for data generation */
  case class Params(size: Int, rand: RandomGenerator) {
    def resize(newSize: Int) = Params(newSize,rand)
  }



  // Internal support functions

  private def mkGen[T](g: Params => Option[T]): Gen[T] = new Gen[T] {
    def apply(prms: Params) = g(prms)
  }




  // Generator combinators

  specify("Gen.value", (x: Int, sz: Int) =>
    value(x)(Params(sz,StdRand)).get == x
  )

  /** A generator that always generates a given value */
  def value[T](x: T) = mkGen(p => Some(x))


  specify("Gen.fail", (x: Int, sz: Int) =>
    fail(Params(sz,StdRand)) == None
  )

  /** A generator that never generates a value */
  def fail[T]: Gen[T] = mkGen(p => None)


  specify("Gen.choose", (l: Int, h: Int, sz: Int) => {
    val x = choose(l,h)(Params(sz,StdRand)).get
    h >= l ==> (x >= l && x <= h)
  })

  /** A generator that generates a random integer in the given (inclusive)
   *  range.
   */
  def choose(inclusiveRange: (Int,Int)) =
    parameterized(prms => value(prms.rand.choose(inclusiveRange)))


  /** Creates a generator that can access its generation parameters
   */
  def parameterized[T](f: Params => Gen[T]): Gen[T] =
    mkGen(prms => f(prms)(prms))


  /** Creates a generator that can access its generation size
   */
  def sized[T](f: Int => Gen[T]) = parameterized(prms => f(prms.size))


  /** Creates a resized version of a generator
   */
  def resize[T](s: Int, g: Gen[T]) = mkGen(prms => g(prms.resize(s)))


  /** Chooses one of the given generators, with a weighted random distribution.
   */
  def frequency[T](gs: (Int,Gen[T])*): Gen[T] = {
    val tot = (gs.map(_._1) :\ 0) (_+_)

    def pick(n: Int, l: List[(Int,Gen[T])]): Gen[T] = l match {
      case Nil => fail
      case (k,g)::gs => if(n <= k) g else pick(n-k, gs)
    }

    for {
      n <- choose(1,tot)
      x <- pick(n,gs.toList)
    } yield x
  }


  specify("Gen.elements", (l: List[Int], sz: Int) =>
    elements(l: _*)(Params(sz,StdRand)) match {
      case None => l.isEmpty
      case Some(n) => l.contains(n)
    }
  )

  /** A generator that returns a random element from a list
   */
  def elements[T](xs: T*): Gen[T] = if(xs.isEmpty) fail else for {
    i <- choose((0,xs.length-1))
  } yield xs(i)


  /** Picks a random generator from a list */
  def oneOf[T](gs: Seq[Gen[T]]) = if(gs.isEmpty) fail else for {
    i <- choose((0,gs.length-1))
    x <- gs(i)
  } yield x


  /** Generates a list of random length. The maximum length depends on the
   *  size parameter
   */
  def listOf[T](g: Gen[T]) = sized(size => for {
    n <- choose(0,size)
    l <- vectorOf(n,g)
  } yield l.toList)


  /** Generates a non-empty list of random length. The maximum length depends
   *  on the size parameter
   */
  def listOf1[T](g: Gen[T]) = for {
    x  <- g
    xs <- listOf(g)
  } yield x::xs


  specify("Gen.vectorOf", (len: Int, sz: Int) =>
    () imply {
      case () if len == 0 =>
        vectorOf(len,fail)(Params(sz,StdRand)).get.length == 0 &&
        vectorOf(len,value(0))(Params(sz,StdRand)).get.length == 0
      case () if len > 0 =>
        vectorOf(len,fail)(Params(sz,StdRand)) == None &&
        vectorOf(len,value(0))(Params(sz,StdRand)).get.length == len
    }
  )

  /** Generates a list of the given length */
  def vectorOf[T](n: Int, g: Gen[T]) = new Gen[Seq[T]] {
    def apply(prms: Params): Option[Seq[T]] = {
      val l = new ListBuffer[T]
      var i = 0
      while(i < n) g(prms) match {
        case Some(x) => 
          l += x
          i += 1
        case None => return None
      }
      Some(l)
    }
  }

}
