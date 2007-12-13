package org.scalacheck

import scala.collection.mutable.ListBuffer

/** A property is a generator that generates a property result */
class Prop(g: Gen.Params => Option[Prop.Result]) extends Gen[Prop.Result](g) {

  import Prop.{True,False,Exception}

  /** Returns a new property that holds if and only if both this
   *  and the given property hold. If one of the properties doesn't
   *  generate a result, the new property will generate false.
   */
  def &&(p: Prop): Prop = combine(p) {
    case (x, Some(True(_))) => x
    case (Some(True(_)), x) => x

    case (x@Some(False(_)), _) => x
    case (_, x@Some(False(_))) => x

    case (None, x) => x
    case (x, None) => x

    case (x, _) => x
  }

  /** Returns a new property that holds if either this
   *  or the given property (or both) hold.
   */
  def ||(p: Prop): Prop = combine(p) {
    case (x@Some(True(_)), _) => x
    case (_, x@Some(True(_))) => x

    case (Some(False(_)), x) => x
    case (x, Some(False(_))) => x

    case (None, x) => x
    case (x, None) => x

    case (x, _) => x
  }

  /** Returns a new property that holds if and only if both this
   *  and the given property hold. If one of the properties doesn't
   *  generate a result, the new property will generate the same result
   *  as the other property.
   */
  def ++(p: Prop): Prop = combine(p) {
    case (None, x) => x
    case (x, None) => x

    case (x, Some(True(_))) => x
    case (Some(True(_)), x) => x

    case (x@Some(False(_)), _) => x
    case (_, x@Some(False(_))) => x

    case (x, _) => x
  }

  /** Returns a new property that holds if and only if both this
   *  and the given property generates the same result.
   */
  def ==(p: Prop) = new Prop(prms =>
    (this(prms), p(prms)) match {
      case (None,None) => Prop.proved(prms)
      case (Some(r1),Some(r2)) if r1 == r2 => Prop.proved(prms)
      case _ => Prop.falsified(prms)
    }
  )

  def addArg(a: Arg) = new Prop(prms =>
    this(prms) match {
      case None => None
      case Some(r) => Some(r.addArg(a))
    }
  ) { override def toString = Prop.this.toString }

}

object Prop extends Properties {

  import Gen.{value, fail, frequency, elements}
  import Arbitrary._

  // Properties for the Prop class

  specify("Prop.Prop.&& Commutativity", (p1: Prop, p2: Prop) =>
    (p1 && p2) == (p2 && p1)
  )
  specify("Prop.Prop.&& Identity", (p: Prop) =>
    (p && proved) == p
  )
  specify("Prop.Prop.&& False", (p: Prop) =>
    (p && falsified) == falsified
  )
  specify("Prop.Prop.&& Right prio", (sz: Int) => {
    val p = proved.addArg(Arg("","RHS",0)) && proved.addArg(Arg("","LHS",0))
    p(Gen.Params(sz,StdRand)) match {
      case Some(r) if r.args == Arg("","RHS",0)::Nil => true
      case _ => false
    }
  })

  specify("Prop.Prop.|| Commutativity", (p1: Prop, p2: Prop) =>
    (p1 || p2) == (p2 || p1)
  )
  specify("Prop.Prop.|| Identity", (p: Prop) =>
    (p || falsified) == p
  )
  specify("Prop.Prop.|| True", (p: Prop) =>
    (p || proved) == proved
  )

  specify("Prop.Prop.++ Commutativity", (p1: Prop, p2: Prop) =>
    (p1 ++ p2) == (p2 ++ p1)
  )
  specify("Prop.Prop.++ Identity", (p: Prop) =>
    (p ++ undecided) == p
  )
  specify("Prop.Prop.++ False", (p: Prop) =>
    (p ++ falsified) == falsified
  )
  specify("Prop.Prop.++ True", {
    val g = elements(proved,falsified,exception(null))
    forAll(g)(p => (p ++ proved) == p)
  })



  // Types

  /** The result of evaluating a property */
  abstract sealed class Result(val args: List[Arg]) {
    override def equals(x: Any) = (this,x) match {
      case (True(_),True(_))   => true
      case (False(_),False(_)) => true
      case (Exception(_,_),Exception(_,_)) => true
      case _ => false
    }

    def success = this match {
      case _:True => true
      case _ => false
    }

    def failure = this match {
      case _:False => true
      case _:Exception => true
      case _ => false
    }

    def addArg(a: Arg) = this match {
      case True(as) => True(a::as)
      case False(as) => False(a::as)
      case Exception(as,e) => Exception(a::as,e)
    }

  }

  /** The property was true with the given arguments */
  case class True(as: List[Arg]) extends Result(as)

  /** The property was false with the given arguments */
  case class False(as: List[Arg]) extends Result(as)

  /** Evaluating the property with the given arguments raised an exception */
  case class Exception(as: List[Arg], e: Throwable) extends Result(as)



  // Private support functions

  private def constantProp(r: Option[Result], descr: String) =
    new Prop(prms => r) { override def toString = descr }

  private implicit def genToProp(g: Gen[Result]) = new Prop(g.apply)


  // Property combinators

  /** A property that never is proved or falsified */
  def undecided: Prop = constantProp(None, "Prop.undecided")

  /** A property that always is false */
  def falsified: Prop = constantProp(Some(False(Nil)), "Prop.falsified")

  /** A property that always is true */
  def proved: Prop = constantProp(Some(True(Nil)), "Prop.proved");

  /** A property that denotes an exception */
  def exception(e: Throwable): Prop =
    constantProp(Some(Exception(Nil,e)), "Prop.exception")

  /** A property that depends on the generator size */
  def sizedProp(f: Int => Prop): Prop = new Prop(prms => f(prms.size)(prms))

  /** Implication */
  def ==>(b: => Boolean, p: => Prop): Prop = property(if (b) p else undecided)

  /** Implication with several conditions */
  def imply[T](x: T, f: PartialFunction[T,Prop]): Prop =
    property(if(f.isDefinedAt(x)) f(x) else undecided)

  def iff[T](x: T, f: PartialFunction[T,Prop]): Prop =
    property(if(f.isDefinedAt(x)) f(x) else falsified)

  /** Combines properties into one, which is true if and only if all the
   *  properties are true
   */
  def all(ps: Seq[Prop]) = (proved /: ps) (_ && _)

  /** Universal quantifier */
  def forAll[A,P](g: Gen[A])(f: A => Prop): Prop = for {
    a <- g
    r <- property(f(a))
  } yield r.addArg(Arg(g.label,a,0))

  /** Universal quantifier, shrinks failed arguments with given shrink
   *  function */
  def forAllShrink[A](g: Gen[A],shrink: A => Stream[A])(f: A => Prop) =
    new Prop((prms: Gen.Params) => {

      import Stream._

      def getFirstFail(xs: Stream[A], shrinks: Int) = {
        val results = xs.map(x =>
          f(x)(prms).map(r => (x, r.addArg(Arg(g.label,x,shrinks)))))
        results match {
          case Stream.empty => None
          case _ => results.dropWhile(!isFailure(_)) match {
            case Stream.empty => results.head
            case failures => failures.head
          }
        }
      }

      def isFailure(r: Option[(A,Result)]) = r match {
        case Some((_,res)) => res.failure
        case _ => false
      }

      g(prms) match {
        case None => None
        case Some(x) =>
          var shrinks = 0
          var xr = getFirstFail(cons(x, empty), shrinks)
          if(!isFailure(xr)) xr.map(_._2)
          else {
            var r: Option[Result] = None
            do {
              shrinks += 1
              r = xr.map(_._2)
              xr = getFirstFail(shrink(xr.get._1), shrinks)
            } while(isFailure(xr))
            r
          }
      }
    })

  /** Universal quantifier, shrinks failed arguments with the default
   *  shrink function for the type */
  def forAllDefaultShrink[A](g: Gen[A])(f: A => Prop)
    (implicit a: Arb[A] => Arbitrary[A]) = forAllShrink(g,shrink[A])(f)


  class ExtendedBoolean(b: Boolean) {
    /** Implication */
    def ==>(p: => Prop) = Prop.==>(b,p)
  }

  class ExtendedAny[T](x: T) {
    /** Implication with several conditions */
    def imply(f: PartialFunction[T,Prop]) = Prop.imply(x,f)
    def iff(f: PartialFunction[T,Prop]) = Prop.iff(x,f)
  }



  // Implicit defs

  implicit def extendedBoolean(b: Boolean) = new ExtendedBoolean(b)
  implicit def extendedAny[T](x: T) = new ExtendedAny(x)



  // Implicit properties for common types

  implicit def propBoolean(b: Boolean): Prop = if(b) proved else falsified


  def property (p: => Prop): Prop = new Prop(prms =>
    (try { p } catch { case e => exception(e) })(prms)
  )

  def property[A1,P] (
    f:  A1 => P)(implicit
    p: P => Prop,
    a1: Arb[A1] => Arbitrary[A1]
  ) = forAllShrink(arbitrary[A1],shrink[A1])(f andThen p)

  def property[A1,A2,P] (
    f:  (A1,A2) => P)(implicit
    p: P => Prop,
    a1: Arb[A1] => Arbitrary[A1],
    a2: Arb[A2] => Arbitrary[A2]
  ): Prop = property((a: A1) => property(f(a, _:A2)))

  def property[A1,A2,A3,P] (
    f:  (A1,A2,A3) => P)(implicit
    p: P => Prop,
    a1: Arb[A1] => Arbitrary[A1],
    a2: Arb[A2] => Arbitrary[A2],
    a3: Arb[A3] => Arbitrary[A3]
  ): Prop = property((a: A1) => property(f(a, _:A2, _:A3)))

  def property[A1,A2,A3,A4,P] (
    f:  (A1,A2,A3,A4) => P)(implicit
    p: P => Prop,
    a1: Arb[A1] => Arbitrary[A1],
    a2: Arb[A2] => Arbitrary[A2],
    a3: Arb[A3] => Arbitrary[A3],
    a4: Arb[A4] => Arbitrary[A4]
  ): Prop = property((a: A1) => property(f(a, _:A2, _:A3, _:A4)))

  def property[A1,A2,A3,A4,A5,P] (
    f:  (A1,A2,A3,A4,A5) => P)(implicit
    p: P => Prop,
    a1: Arb[A1] => Arbitrary[A1],
    a2: Arb[A2] => Arbitrary[A2],
    a3: Arb[A3] => Arbitrary[A3],
    a4: Arb[A4] => Arbitrary[A4],
    a5: Arb[A5] => Arbitrary[A5]
  ): Prop = property((a: A1) => property(f(a, _:A2, _:A3, _:A4, _:A5)))

  def property[A1,A2,A3,A4,A5,A6,P] (
    f:  (A1,A2,A3,A4,A5,A6) => P)(implicit
    p: P => Prop,
    a1: Arb[A1] => Arbitrary[A1],
    a2: Arb[A2] => Arbitrary[A2],
    a3: Arb[A3] => Arbitrary[A3],
    a4: Arb[A4] => Arbitrary[A4],
    a5: Arb[A5] => Arbitrary[A5],
    a6: Arb[A6] => Arbitrary[A6]
  ): Prop = property((a: A1) => property(f(a, _:A2, _:A3, _:A4, _:A5, _:A6)))

}
