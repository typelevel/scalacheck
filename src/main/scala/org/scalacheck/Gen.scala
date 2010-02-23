/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2009 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*-------------------------------------------------------------------------*/

package org.scalacheck

import scala.collection.mutable.ListBuffer
import util.Buildable
import Prop._
import Arbitrary._

/** Class that represents a generator. */
sealed trait Gen[+T] {

  import Gen.choose

  var label = "" // TODO: Ugly mutable field

  /** Put a label on the generator to make test reports clearer */
  def label(l: String): Gen[T] = {
    label = l
    this
  }

  /** Put a label on the generator to make test reports clearer */
  def :|(l: String) = label(l)

  /** Put a label on the generator to make test reports clearer */
  def |:(l: String) = label(l)

  /** Put a label on the generator to make test reports clearer */
  def :|(l: Symbol) = label(l.toString.drop(1))

  /** Put a label on the generator to make test reports clearer */
  def |:(l: Symbol) = label(l.toString.drop(1))

  def apply(prms: Gen.Params): Option[T]

  def map[U](f: T => U): Gen[U] = Gen(prms => this(prms).map(f)).label(label)

  def map2[U, V](g: Gen[U])(f: (T, U) => V) =
    combine(g)((t, u) => t.flatMap(t => u.flatMap(u => Some(f(t, u)))))

  def map3[U, V, W](gu: Gen[U], gv: Gen[V])(f: (T, U, V) => W) =
    combine3(gu, gv)((t, u, v) => t.flatMap(t => u.flatMap(u => v.flatMap(v => Some(f(t, u, v))))))

  def map4[U, V, W, X](gu: Gen[U], gv: Gen[V], gw: Gen[W])(f: (T, U, V, W) => X) =
    combine4(gu, gv, gw)((t, u, v, w) => t.flatMap(t => u.flatMap(u => v.flatMap(v => w.flatMap(w => Some(f(t, u, v, w)))))))

  def map5[U, V, W, X, Y](gu: Gen[U], gv: Gen[V], gw: Gen[W], gx: Gen[X])(f: (T, U, V, W, X) => Y) =
    combine5(gu, gv, gw, gx)((t, u, v, w, x) => t.flatMap(t => u.flatMap(u => v.flatMap(v => w.flatMap(w => x.flatMap(x => Some(f(t, u, v, w, x))))))))

  def map6[U, V, W, X, Y, Z](gu: Gen[U], gv: Gen[V], gw: Gen[W], gx: Gen[X], gy: Gen[Y])(f: (T, U, V, W, X, Y) => Z) =
    combine6(gu, gv, gw, gx, gy)((t, u, v, w, x, y) => t.flatMap(t => u.flatMap(u => v.flatMap(v => w.flatMap(w => x.flatMap(x => y.flatMap(y => Some(f(t, u, v, w, x, y)))))))))

  def flatMap[U](f: T => Gen[U]): Gen[U] = Gen(prms => for {
    t <- this(prms)
    u <- f(t)(prms)
  } yield u)

  def filter(p: T => Boolean): Gen[T] = Gen(prms => for {
    t <- this(prms)
    u <- if (p(t)) Some(t) else None
  } yield u).label(label)

  def suchThat(p: T => Boolean): Gen[T] = filter(p)

  def combine[U,V](g: Gen[U])(f: (Option[T],Option[U]) => Option[V]): Gen[V] =
    Gen(prms => f(this(prms), g(prms)))

  def combine3[U, V, W](gu: Gen[U], gv: Gen[V])
      (f: (Option[T], Option[U], Option[V]) => Option[W]) =
    Gen(prms => f(this(prms), gu(prms), gv(prms)))

  def combine4[U, V, W, X](gu: Gen[U], gv: Gen[V], gw: Gen[W])
      (f: (Option[T], Option[U], Option[V], Option[W]) => Option[X]) =
    Gen(prms => f(this(prms), gu(prms), gv(prms), gw(prms)))

  def combine5[U, V, W, X, Y](gu: Gen[U], gv: Gen[V], gw: Gen[W], gx: Gen[X])
      (f: (Option[T], Option[U], Option[V], Option[W], Option[X]) => Option[Y]) =
    Gen(prms => f(this(prms), gu(prms), gv(prms), gw(prms), gx(prms)))

  def combine6[U, V, W, X, Y, Z](gu: Gen[U], gv: Gen[V], gw: Gen[W], gx: Gen[X], gy: Gen[Y])
      (f: (Option[T], Option[U], Option[V], Option[W], Option[X], Option[Y]) => Option[Z]) =
        Gen(prms => f(this(prms), gu(prms), gv(prms), gw(prms), gx(prms), gy(prms)))

  def ap[U](g: Gen[T => U]) = flatMap(t => g.flatMap(u => Gen(p => Some(u(t)))))

  override def toString =
    if(label.length == 0) "Gen()" else "Gen(\"" + label + "\")"

  /** Returns a new property that holds if and only if both this
   *  and the given generator generates the same result, or both
   *  generators generate no result.
   *  @deprecated Use <code>==</code> instead */
  @deprecated("Use == instead")
  def ===[U](g: Gen[U]): Prop = this == g

  /** Returns a new property that holds if and only if both this
   *  and the given generator generates the same result, or both
   *  generators generate no result.  */
  def ==[U](g: Gen[U]) = Prop(prms =>
    (this(prms.genPrms), g(prms.genPrms)) match {
      case (None,None) => proved(prms)
      case (Some(r1),Some(r2)) if r1 == r2 => proved(prms)
      case _ => falsified(prms)
    }
  )

  def !=[U](g: Gen[U]) = forAll(this)(r => forAll(g)(_ != r))

  def !==[U](g: Gen[U]) = Prop(prms =>
    (this(prms.genPrms), g(prms.genPrms)) match {
      case (None,None) => falsified(prms)
      case (Some(r1),Some(r2)) if r1 == r2 => falsified(prms)
      case _ => proved(prms)
    }
  )

  def |[U >: T](g: Gen[U]): Gen[U] = 
    choose(0,1).flatMap(n => if(n == 0) g else this)

  /** Generates a sample value by using default parameters */
  def sample: Option[T] = apply(Gen.defaultParams)

}


/** Contains combinators for building generators. */
object Gen {

  import Arbitrary._
  import Shrink._

  /** Record that encapsulates all parameters required for data generation */
  case class Params(size: Int, rng: java.util.Random) {
    def resize(newSize: Int) = Params(newSize,rng)

    /** @throws IllegalArgumentException if l is greater than h. */
    def choose(l: Int, h: Int): Int = choose(l:Long, h:Long).toInt

    /** @throws IllegalArgumentException if l is greater than h, or if
    *  the range between l and h doesn't fit in a Long. */
    def choose(l: Long, h: Long): Long = {
      val d = h-l+1
      
      if (d <= 0) throw new IllegalArgumentException
      else l + (Math.abs(rng.nextLong) % d)
    }

    def choose(l: Double, h: Double) = {
      if (h <= l) h
      else rng.nextDouble * (h-l) + l
    }

  }

  /* Default generator parameters */
  val defaultParams = Params(100, util.StdRand)

  /* Generator factory method */
  def apply[T](g: Gen.Params => Option[T]) = new Gen[T] { 
    def apply(p: Gen.Params) = g(p) 
  }

  /* Convenience method for using the <code>frequency</code> method like this:
   * <code>frequency((1, "foo"), (3, "bar"))</code> */
  implicit def freqTuple[T](t: (Int, T)): (Int, Gen[T]) = (t._1, value(t._2))


  //// Various Generator Combinators ////

  /** Sequences generators. If any of the given generators fails, the
   *  resulting generator will also fail. */
  def sequence[C[_],T](gs: Iterable[Gen[T]])(implicit b: Buildable[C]): Gen[C[T]] = Gen(prms => {
    val builder = b.builder[T]
    var none = false
    val xs = gs.iterator
    while(xs.hasNext && !none) xs.next.apply(prms) match {
      case None => none = true
      case Some(x) => builder += x
    }
    if(none) None else Some(builder.finalise)
  })

  /** Wraps a generator lazily. The given parameter is only evalutated once,
   *  and not until the wrapper generator is evaluated. */
  def lzy[T](g: => Gen[T]) = new Gen[T] {
    lazy val h = g
    def apply(prms: Params) = h(prms)
  }

  /** Wraps a generator for later evaluation. The given parameter is
   *  evaluated each time the wrapper generator is evaluated. */
  def wrap[T](g: => Gen[T]) = Gen(p => g(p))

  /** A generator that always generates the given value */
  implicit def value[T](x: T) = Gen(p => Some(x))

  /** A generator that never generates a value */
  def fail[T]: Gen[T] = Gen(p => None)

  /** A generator that generates a random Integer in the given (inclusive)
   *  range. */
  def choose(low: Int, high: Int) = if(low > high) fail else
    parameterized(prms => value(prms.choose(low,high)))

  /** A generator that generates a random Long in the given (inclusive)
   *  range. */
  def choose(low: Long, high: Long) = if(low > high) fail else
    parameterized(prms => value(prms.choose(low,high)))

  /** A generator that generates a random Double in the given (inclusive)
   *  range. */
  def choose(low: Double, high: Double) = if (low > high) fail else
    parameterized(prms => value(prms.choose(low,high)))

  /** Creates a generator that can access its generation parameters */
  def parameterized[T](f: Params => Gen[T]): Gen[T] = Gen(prms => f(prms)(prms))

  /** Creates a generator that can access its generation size */
  def sized[T](f: Int => Gen[T]) = parameterized(prms => f(prms.size))

  /** Creates a resized version of a generator */
  def resize[T](s: Int, g: Gen[T]) = Gen(prms => g(prms.resize(s)))

  /** Chooses one of the given generators with a weighted random distribution */
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

  /** Picks a random generator from a list */
  def oneOf[T](gs: Gen[T]*) = if(gs.isEmpty) fail else for {
    i <- choose(0,gs.length-1)
    x <- gs(i)
  } yield x

  /** Chooses one of the given values, with a weighted random distribution. 
   *  @deprecated Use <code>frequency</code> with constant generators 
   *  instead. */
  @deprecated("Use 'frequency' with constant generators instead.")
  def elementsFreq[T](vs: (Int, T)*): Gen[T] =
    frequency(vs.map { case (w,v) => (w, value(v)) } : _*)

  /** A generator that returns a random element from a list 
   *  @deprecated Use <code>oneOf</code> with constant generators instead. */
  @deprecated("Use 'oneOf' with constant generators instead.")
  def elements[T](xs: T*): Gen[T] = if(xs.isEmpty) fail else for {
    i <- choose(0,xs.length-1)
  } yield xs(i)


  //// List Generators ////

  /** Generates a container of any type for which there exists an implicit 
   *  <code>Buildable</code> instance. The elements in the container will
   *  be generated by the given generator. The size of the generated container
   *  is given by <code>n</code>. */
  def containerOfN[C[_],T](n: Int, g: Gen[T])(implicit b: Buildable[C]
  ): Gen[C[T]] = sequence[C,T](new Iterable[Gen[T]] {
    def iterator = new Iterator[Gen[T]] {
      var i = 0
      def hasNext = i < n
      def next = { i += 1; g }
    }
  })

  /** Generates a container of any type for which there exists an implicit 
   *  <code>Buildable</code> instance. The elements in the container will
   *  be generated by the given generator. The size of the container is
   *  bounded by the size parameter used when generating values. */
  def containerOf[C[_],T](g: Gen[T])(implicit b: Buildable[C]): Gen[C[T]] = 
    sized(size => for(n <- choose(0,size); c <- containerOfN[C,T](n,g)) yield c)

  /** Generates a non-empty container of any type for which there exists an
   *  implicit <code>Buildable</code> instance. The elements in the container
   *  will be generated by the given generator. The size of the container is
   *  bounded by the size parameter used when generating values. */
  def containerOf1[C[_],T](g: Gen[T])(implicit b: Buildable[C]): Gen[C[T]] = 
    sized(size => for(n <- choose(1,size); c <- containerOfN[C,T](n,g)) yield c)

  /** Generates a list of random length. The maximum length depends on the
   *  size parameter. This method is equal to calling 
   *  <code>containerOf[List,T](g)</code>. */
  def listOf[T](g: => Gen[T]) = containerOf[List,T](g)

  /** Generates a non-empty list of random length. The maximum length depends
   *  on the size parameter. This method is equal to calling 
   *  <code>containerOf1[List,T](g)</code>. */
  def listOf1[T](g: => Gen[T]) = containerOf1[List,T](g)

  /** Generates a list of the given length. This method is equal to calling 
   *  <code>containerOfN[List,T](n,g)</code>. */
  def listOfN[T](n: Int, g: Gen[T]) = containerOfN[List,T](n,g)

  /** Generates a list of the given length. This method is equal to calling 
   *  <code>containerOfN[List,T](n,g)</code>. 
   *  @deprecated Use the method <code>listOfN</code> instead. */
  @deprecated("Use 'listOfN' instead.")
  def vectorOf[T](n: Int, g: Gen[T]) = containerOfN[List,T](n,g)

  /** A generator that picks a random number of elements from a list */
  def someOf[T](l: Iterable[T]) = choose(0,l.size) flatMap (pick(_,l))

  /** A generator that picks a given number of elements from a list, randomly */
  def pick[T](n: Int, l: Iterable[T]): Gen[Seq[T]] =
    if(n > l.size || n < 0) fail
    else Gen(prms => {
      val buf = new ListBuffer[T]
      buf ++= l
      while(buf.length > n) buf.remove(choose(0,buf.length-1)(prms).get)
      Some(buf)
    })


  //// Character Generators ////

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

  //// String Generators ////

  /* Generates a string that starts with a lower-case alpha character,
   * and only contains alphanumerical characters */
  def identifier: Gen[String] = for {
    c <- alphaLowerChar
    cs <- listOf(alphaNumChar)
  } yield (c::cs).mkString

  /* Generates a string of alpha characters */
  def alphaStr: Gen[String] = for(cs <- listOf(Gen.alphaChar)) yield cs.mkString

  /* Generates a string of digits */
  def numStr: Gen[String] = for(cs <- listOf(Gen.numChar)) yield cs.mkString

  //// Number Generators ////

  /* Generates positive integers */
  def posInt: Gen[Int] = sized(max => choose(1, max))

  /* Generates negative integers */
  def negInt: Gen[Int] = sized(max => choose(-max, -1))
  
  private val MAXBITS = 20
  private def basicGenerators[T](minT: T, maxT: T)(implicit num: Numeric[T]): List[(Int, Gen[T])] = {
    import num._
    
    List(
      (1, minT),
      (1, maxT),
      (1, zero),
      (1, one),
      (1, -one)
    )
  }
  private def bitWidthGenerators[T](bits: Int, chooser: (T, T) => Gen[T])(implicit num: Numeric[T]): Option[(Int, Gen[T])] = {
    import num._
    
    def rangedGeneratorFromBits(b: Int): Option[Gen[T]] = {
      val x1 = fromInt(1 << b)
      val x2 = fromInt(1 << (b + 1))
      if (x2 <= x1) None
      else Some(chooser(x1, x2))
    }
    
    rangedGeneratorFromBits(bits) map (x => (1, x))
  }
  
  /** Creates plausibly well-rounded generator for Byte/Short/Int/Long, including
    * zero, +/- unity, and both extremities.
    */
  def integralGenerator[T](minT: T, maxT: T, chooser: (T, T) => Gen[T])(implicit num: Integral[T]): Gen[T] = {
    import num._

    // count the number of bits in this representation
    def bits(x: T, acc: Int): Int = {
      val next = x * (one + one)
      if (next < x) acc // overflow
      else bits(next, acc + 1)
    }
    def getMaxBits = bits(one, 0) min MAXBITS
    
    // generators for 2 to 4, 2 to 8, 2 to 16, etc. so as to weight toward
    // more likely to be relevant numbers
    val weightedGens: List[(Int, Gen[T])] =
      for (b <- (1 to getMaxBits).toList ; g <- bitWidthGenerators(b, chooser)) yield g
    
    // (1 to getMaxBits).toList map (bitWidthGenerators(_, chooser))
    
    val args = basicGenerators(minT, maxT) ++ weightedGens ++ List(
      (10, chooser(minT + one, -one)),
      (10, chooser(one, maxT - one))
    )
    
    frequency(args: _*)
  }
  
  /** Like integralGenerator for Float/Double - also makes sure to explore the -1.0 to 1.0 range. */
  def fractionalGenerator[T](minT: T, maxT: T, chooser: (T, T) => Gen[T], specials: T*)(implicit num: Fractional[T]): Gen[T] = {
    import num._
    
    val extras: List[(Int, Gen[T])] = 
      (specials.toList map (s => (1, value(s)))) ++
      ((1 to 20).toList map (bitWidthGenerators(_, chooser).get))
    
    val args = basicGenerators(minT, maxT) ++ extras ++ List(
      (5, chooser(-one, zero)),
      (5, chooser(zero, one)),
      (10, chooser(minT + one, -one)),
      (10, chooser(one, maxT - one))  
    )
    
    frequency(args: _*)
  }
  
  //// AnyVal Generators ////
  def genUnit: Gen[Unit]      = value(())
  def genBool: Gen[Boolean]   = oneOf(true, false)
  def genChar: Gen[Char]      = choose(Char.MinValue, Char.MaxValue) map (_.toChar)
  def genByte: Gen[Byte]      = integralGenerator(Byte.MinValue, Byte.MaxValue, choose(_: Byte, _: Byte) map (_.toByte))
  def genShort: Gen[Short]    = integralGenerator(Short.MinValue, Short.MaxValue, choose(_: Short, _: Short) map (_.toShort))
  def genInt: Gen[Int]        = integralGenerator(Int.MinValue, Int.MaxValue, choose(_: Int, _: Int))
  def genLong: Gen[Long]      = integralGenerator(Long.MinValue >> 2, Long.MaxValue >> 2, choose(_: Long, _: Long))
  def genFloat: Gen[Float]    = fractionalGenerator(
    Float.MinValue, Float.MaxValue,
    (x: Float, y: Float) => choose(x.toDouble, y.toDouble) map (_.toFloat)
    // I find that including these by default is a little TOO testy.
    // Float.Epsilon, Float.NaN, Float.PositiveInfinity, Float.NegativeInfinity
  )
  def genDouble: Gen[Double]  = fractionalGenerator(
    Double.MinValue, Double.MaxValue,
    choose(_: Double, _: Double)
    // As above.  Perhaps behind some option?
    // Double.Epsilon, Double.NaN, Double.PositiveInfinity, Double.NegativeInfinity
  )
  def genAnyVal: Gen[AnyVal]  = oneOf(genUnit, genBool, genChar, genByte, genShort, genInt, genLong, genFloat, genDouble)
}
