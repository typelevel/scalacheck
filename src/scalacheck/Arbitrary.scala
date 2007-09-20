package scalacheck

/** Most of the arbitrary generators and shrinkers defined in this file
 *  are straightforward adaptations of the ones in Arbitrary.hs, from
 *  QuickCheck 2.
 */



/** Dummy type that helps Scala's type inference a bit. */
sealed class Arb[T] {}


/** The Arbitrary[T] class represents a type T that can be instantiated
 *  arbitrarily.
 *  To make your own "instance" of the Arbitrary class for a type T, define an
 *  implicit function that takes a value of type Arb[T] and returns a
 *  value of type Arbitrary[T]. The Arb[T] value has no meaning in itself,
 *  its just there to make type inference work a bit better. Do not try to use
 *  the Arb[T] value in your implicit function, it will always be null.
 */
abstract class Arbitrary[T] {
  protected def getArbitrary: Gen[T]
  protected def getShrink(x: T): Stream[T] = Stream.empty
}


/** Contains Arbitrary instances for common types. */
object Arbitrary {

  import Gen.{value, choose, sized, elements, listOf, listOf1,
    frequency}

  /** Arbitrary instance of value of type T. */
  def arbitrary[T](implicit a: Arb[T] => Arbitrary[T]): Gen[T] =
    a(null).getArbitrary

  /** Shrinks a generated value */
  def shrink[T](x: T)(implicit a: Arb[T] => Arbitrary[T]): Stream[T] =
    a(null).getShrink(x)


  // Arbitrary instances for common types

  /** Arbitrary instance of bool */
  implicit def arbitraryBool(x: Arb[Boolean]) = new Arbitrary[Boolean] {
    def getArbitrary = elements(true,false)
  }

  /** Arbitrary instance of integer */
  implicit def arbitraryInt(x: Arb[Int]) = new Arbitrary[Int] {
    def getArbitrary = sized (s => choose(-s,s))

    override def getShrink(n: Int) = {

      def iterate[T](f: T => T, x: T): Stream[T] = {
        val y = f(x)
        Stream.cons(y, iterate(f,y))
      }

      if(n == 0) Stream.empty
      else {
        val ns = Stream.cons(0, iterate((_:Int)/2, n).takeWhile(_ != 0).map(n - _))
        if(n < 0) Stream.cons(-n,ns) else ns
      }
    }
  }

  /** Arbitrary instance of Double */
  implicit def arbitraryDouble(x: Arb[Double]) = new Arbitrary[Double] {
    def getArbitrary = sized (s => choose(-s: Double, s: Double))
  }

  /** Arbitrary instance of char */
  implicit def arbitraryChar(x: Arb[Char]) = new Arbitrary[Char] {
    def getArbitrary = choose(0,255) map (_.toChar)
  }

  /** Arbitrary instance of string */
  implicit def arbitraryString(x: Arb[String]): Arbitrary[String] =
    new Arbitrary[String] {
      def getArbitrary = arbitrary[List[Char]] map (List.toString(_))
    }

  /** Arbitrary instance of Gen */
  implicit def arbitraryGen[T](
    x: Arb[Gen[T]])(implicit
    a: Arb[T] => Arbitrary[T]
  ): Arbitrary[Gen[T]] = new Arbitrary[Gen[T]] {
    def getArbitrary = arbitrary[T] map (value(_))
  }

  /** Generates an arbitrary property */
  implicit def arbitraryProp(x: Arb[Prop]) = new Arbitrary[Prop] {
    def getArbitrary = frequency(
      (5, value(Prop.proved)),
      (4, value(Prop.falsified)),
      (2, value(Prop.undecided)),
      (1, value(Prop.exception(null)))
    )
  }

  /** Arbitrary instance of test params */
  implicit def arbitraryTestParams(x: Arb[Test.Params]) =
    new Arbitrary[Test.Params] {
      def getArbitrary = for {
        minSuccessfulTests <- choose(10,150)
        maxDiscardedTests  <- choose(100,500)
        minSize <- choose(0,500)
        sizeDiff <- choose(0,500)
        maxSize <- choose(minSize, minSize + sizeDiff)
      } yield Test.Params(minSuccessfulTests,maxDiscardedTests,minSize,
                          maxSize,StdRand)
    }

  /** Arbitrary instance of gen params */
  implicit def arbitraryGenParams(x: Arb[Gen.Params]): Arbitrary[Gen.Params] =
    new Arbitrary[Gen.Params] {
      def getArbitrary = for {
        size <- arbitrary[Int] suchThat (_ >= 0)
      } yield Gen.Params(size, StdRand)
    }

  /** Arbitrary instance of List. The maximum length of the list
   *  depends on the size parameter. */
  implicit def arbitraryList[T](
    x: Arb[List[T]])(implicit
    a: Arb[T] => Arbitrary[T]
  ): Arbitrary[List[T]] = new Arbitrary[List[T]] {
    def getArbitrary = listOf(arbitrary[T]) map (_.toList)

    override def getShrink(xs: List[T]) = {
      import Stream.cons

      def interleave(xs: Stream[List[T]],ys: Stream[List[T]]
      ): Stream[List[T]] = (xs,ys) match {
        case (Stream.empty,ys) => ys
        case (xs,Stream.empty) => xs
        case (cons(x,xs),cons(y,ys)) => cons(x, cons(y, interleave(xs,ys)))
      }

      def removeChunks(n: Int, xs: List[T]): Stream[List[T]] = xs match {
        case Nil => Stream.empty
        case _::Nil => cons(Nil, Stream.empty)
        case _ =>
          val n1 = n / 2
          val n2 = n - n1
          // TODO: Make all xs vals lazy in Scala 2.6.0
          val xs1 = xs.take(n1)
          val xs2 = xs.drop(n1)
          val xs3 =
            for(ys1 <- removeChunks(n1,xs1) if !ys1.isEmpty) yield ys1 ::: xs2
          val xs4 =
            for(ys2 <- removeChunks(n2,xs2) if !ys2.isEmpty) yield xs1 ::: ys2

          cons(xs1, cons(xs2, interleave(xs3,xs4)))
      }

      def shrinkOne(xs: List[T]): Stream[List[T]] = xs match {
        case Nil => Stream.empty
        case x::xs =>
          (for(y <- shrink(x)) yield y::xs) append
          (for(ys <- shrinkOne(xs)) yield x::ys)
      }

      removeChunks(xs.length,xs).append(shrinkOne(xs))
    }
  }

  /** Arbitrary instance of 2-tuple */
  implicit def arbitraryTuple2[T1,T2] (
    x: Arb[Tuple2[T1,T2]])(implicit
    a1: Arb[T1] => Arbitrary[T1],
    a2: Arb[T2] => Arbitrary[T2]
  ): Arbitrary[Tuple2[T1,T2]] = new Arbitrary[Tuple2[T1,T2]] {
    def getArbitrary = for {
      t1 <- arbitrary[T1]
      t2 <- arbitrary[T2]
    } yield (t1,t2)

    override def getShrink(t: (T1,T2)) =
      (for(x1 <- shrink(t._1)) yield (x1, t._2)) append
      (for(x2 <- shrink(t._2)) yield (t._1, x2))
  }

  /** Arbitrary instance of 3-tuple */
  implicit def arbitraryTuple3[T1,T2,T3] (
    x: Arb[Tuple3[T1,T2,T3]])(implicit
    a1: Arb[T1] => Arbitrary[T1],
    a2: Arb[T2] => Arbitrary[T2],
    a3: Arb[T3] => Arbitrary[T3]
  ): Arbitrary[Tuple3[T1,T2,T3]] = new Arbitrary[Tuple3[T1,T2,T3]] {
    def getArbitrary = for {
      t1 <- arbitrary[T1]
      t2 <- arbitrary[T2]
      t3 <- arbitrary[T3]
    } yield (t1,t2,t3)

    override def getShrink(t: (T1,T2,T3)) =
      (for(x1 <- shrink(t._1)) yield (x1, t._2, t._3)) append
      (for(x2 <- shrink(t._2)) yield (t._1, x2, t._3)) append
      (for(x3 <- shrink(t._3)) yield (t._1, t._2, x3))
  }

  /** Arbitrary instance of 4-tuple */
  implicit def arbitraryTuple4[T1,T2,T3,T4] (
    x: Arb[Tuple4[T1,T2,T3,T4]])(implicit
    a1: Arb[T1] => Arbitrary[T1],
    a2: Arb[T2] => Arbitrary[T2],
    a3: Arb[T3] => Arbitrary[T3],
    a4: Arb[T4] => Arbitrary[T4]
  ): Arbitrary[Tuple4[T1,T2,T3,T4]] = new Arbitrary[Tuple4[T1,T2,T3,T4]] {
    def getArbitrary = for {
      t1 <- arbitrary[T1]
      t2 <- arbitrary[T2]
      t3 <- arbitrary[T3]
      t4 <- arbitrary[T4]
    } yield (t1,t2,t3,t4)

    override def getShrink(t: (T1,T2,T3,T4)) =
      (for(x1 <- shrink(t._1)) yield (x1, t._2, t._3, t._4)) append
      (for(x2 <- shrink(t._2)) yield (t._1, x2, t._3, t._4)) append
      (for(x3 <- shrink(t._3)) yield (t._1, t._2, x3, t._4)) append
      (for(x4 <- shrink(t._4)) yield (t._1, t._2, t._3, x4))
  }

}
