package scalacheck

import scala.collection.mutable.ListBuffer

/** Class that represents a generator. */
class Gen[+T](g: Gen.Params => Option[T]) {

  def apply(prms: Gen.Params) = g(prms)

  def map[U](f: T => U): Gen[U] = new Gen(prms => this(prms).map(f))

  def flatMap[U](f: T => Gen[U]): Gen[U] = new Gen(prms => for {
    t <- this(prms)
    u <- f(t)(prms)
  } yield u)

  def filter(p: T => Boolean): Gen[T] = new Gen(prms => for {
    t <- this(prms)
    u <- if (p(t)) Some(t) else None
  } yield u)

  def suchThat(p: T => Boolean): Gen[T] = filter(p)

  def combine[U,V](g: Gen[U])(f: (Option[T],Option[U]) => Option[V]): Gen[V] =
    new Gen(prms => f(this(prms), g(prms)))

}


/** Contains combinators for building generators. */
object Gen extends Testable {

  import Prop._

  // Types

  /** Record that encapsulates all parameters required for data generation */
  case class Params(size: Int, rand: RandomGenerator) {
    def resize(newSize: Int) = Params(newSize,rand)
  }


  // Generator combinators

  specify("Gen.value", (x: Int, prms: Params) =>
    value(x)(prms).get == x
  )

  /** A generator that always generates a given value */
  def value[T](x: T) = new Gen(p => Some(x))


  specify("Gen.fail", (x: Int, prms: Params) =>
    fail(prms) == None
  )

  /** A generator that never generates a value */
  def fail[T]: Gen[T] = new Gen(p => None)


  specify("Gen.choose-int", (l: Int, h: Int, prms: Params) => {
    val x = choose(l,h)(prms).get
    h >= l ==> (x >= l && x <= h)
  })

  /** A generator that generates a random integer in the given (inclusive)
   *  range.  */
  def choose(low: Int, high: Int) =
    parameterized(prms => value(prms.rand.choose(low,high)))


  specify("Gen.choose-double", (l: Double, h: Double, prms: Params) => {
    val x = choose(l,h)(prms).get
    h >= l ==> (x >= l && x <= h)
  })

  /** A generator that generates a random integer in the given (inclusive)
   *  range.  */
  def choose(low: Double, high: Double) =
    parameterized(prms => value(prms.rand.choose(low,high)))


  /** Creates a generator that can access its generation parameters
   */
  def parameterized[T](f: Params => Gen[T]): Gen[T] =
    new Gen(prms => f(prms)(prms))


  /** Creates a generator that can access its generation size
   */
  def sized[T](f: Int => Gen[T]) = parameterized(prms => f(prms.size))


  /** Creates a resized version of a generator
   */
  def resize[T](s: Int, g: Gen[T]) = new Gen(prms => g(prms.resize(s)))


  /** Chooses one of the given generators, with a weighted random distribution.
   */
  def frequency[T](gs: (Int,Gen[T])*): Gen[T] = {
    lazy val tot = (gs.map(_._1) :\ 0) (_+_)

    def pick(n: Int, l: List[(Int,Gen[T])]): Gen[T] = l match {
      case Nil => fail
      case (k,g)::gs => if(n <= k) g else pick(n-k, gs)
    }

    for {
      n <- choose(1,tot)
      x <- pick(n,gs.toList)
    } yield x
  }


  /** Chooses one of the given values, with a weighted random distribution.  */
  def elementsFreq[T](vs: (Int, T)*): Gen[T] = 
    frequency(vs.map { case (w,v) => (w, value(v)) } : _*)


  specify("Gen.elements", (l: List[Int], prms: Params) =>
    elements(l: _*)(prms) match {
      case None => l.isEmpty
      case Some(n) => l.contains(n)
    }
  )

  /** A generator that returns a random element from a list
   */
  def elements[T](xs: T*): Gen[T] = if(xs.isEmpty) fail else for {
    i <- choose(0,xs.length-1)
  } yield xs(i)


  /** Picks a random generator from a list */
  def oneOf[T](gs: Gen[T]*) = if(gs.isEmpty) fail else for {
    i <- choose(0,gs.length-1)
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


  specify("Gen.vectorOf", (len: Int, prms: Params) =>
    () imply {
      case () if len == 0 =>
        vectorOf(len,fail)(prms).get.length == 0 &&
        vectorOf(len,value(0))(prms).get.length == 0
      case () if len > 0 =>
        vectorOf(len,fail)(prms) == None &&
        vectorOf(len,value(0))(prms).get.length == len
    }
  )

  /** Generates a list of the given length */
  def vectorOf[T](n: Int, g: Gen[T]): Gen[Seq[T]] = new Gen(prms => {
    val l = new ListBuffer[T]
    var i = 0
    var break = false
    while(!break && i < n) g(prms) match {
      case Some(x) =>
        l += x
        i += 1
      case None => break = true
    }
    if(break) None
    else Some(l)
  })

  /* Generates a numerical character */
  def numChar: Gen[Char] = choose(48,57) map (_.toChar)

  /* Generates an upper-case alpha character */
  def alphaUpperChar: Gen[Char] = choose(65,90) map (_.toChar)

  /* Generates a lower-case alpha character */
  def alphaLowerChar: Gen[Char] = choose(97,122) map (_.toChar)

  /* Generates an alpha character */
  def alphaChar = frequency((1,alphaUpperChar), (9,alphaLowerChar))

  /* Generates an alphanumerical character */
  def alphaNumChar = frequency((1,numChar), (9,alphaChar))

  /* Generates a string that starts with a lower-case alpha character, 
   * and only contains alphanumerical characters */
  def identifier: Gen[String] = for {
    c <- alphaLowerChar
    cs <- listOf(alphaNumChar)
  } yield List.toString(c::cs)

}
