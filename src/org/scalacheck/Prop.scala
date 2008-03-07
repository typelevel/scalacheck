/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2008 Rickard Nilsson. All rights reserved.          **
**  http://code.google.com/p/scalacheck/                                   **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*-------------------------------------------------------------------------*/

package org.scalacheck

import scala.collection.mutable.ListBuffer

/** A property is a generator that generates a property result */
class Prop(g: Gen.Params => Option[Prop.Result]) extends Gen[Prop.Result](g) {

  import Prop.{Proof,True,False,Exception}

  /** Returns a new property that holds if and only if both this
   *  and the given property hold. If one of the properties doesn't
   *  generate a result, the new property will generate false.
   */
  def &&(p: Prop): Prop = combine(p) {
    case (x@Some(_: Exception), _) => x
    case (_, x@Some(_: Exception)) => x

    case (x, Some(_: Proof)) => x
    case (Some(_: Proof), x) => x

    case (x, Some(_: True)) => x
    case (Some(_: True), x) => x

    case (x@Some(_: False), _) => x
    case (_, x@Some(_: False)) => x

    case _ => None
  }

  /** Returns a new property that holds if either this
   *  or the given property (or both) hold.
   */
  def ||(p: Prop): Prop = combine(p) {
    case (x@Some(_: Exception), _) => x
    case (_, x@Some(_: Exception)) => x

    case (x@Some(_: Proof), _) => x
    case (_, x@Some(_: Proof)) => x

    case (x@Some(_: True), _) => x
    case (_, x@Some(_: True)) => x

    case (Some(_: False), x) => x
    case (x, Some(_: False)) => x

    case _ => None
  }

  /** Returns a new property that holds if and only if both this
   *  and the given property hold. If one of the properties doesn't
   *  generate a result, the new property will generate the same result
   *  as the other property.
   */
  def ++(p: Prop): Prop = combine(p) {
    case (x@Some(_: Exception), _) => x
    case (_, x@Some(_: Exception)) => x

    case (None, x) => x
    case (x, None) => x

    case (x, Some(_: Proof)) => x
    case (Some(_: Proof), x) => x

    case (x, Some(_: True)) => x
    case (Some(_: True), x) => x

    case (x@Some(_: False), _) => x
    case (_, x@Some(_: False)) => x
  }

  /** Returns a new property that holds if and only if both this
   *  and the given property generates the same result, or both
   *  properties generate no result.
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
  ).label(label)

  override def toString = 
    if(label.length == 0) "Prop()" else "Prop(\"" + label + "\")"

}

object Prop extends Properties {

  import Gen.{value, fail, frequency, elements}
  import Arbitrary._
  import Shrink._

  val name = "Prop"


  // Specifications for the Prop class

  specify("Prop.&& Commutativity", (p1: Prop, p2: Prop) =>
    (p1 && p2) === (p2 && p1)
  )
  specify("Prop.&& Exception", (p: Prop) =>
    (p && exception(null)) == exception(null)
  )
  specify("Prop.&& Identity", (p: Prop) =>
    (p && proved) === p
  )
  specify("Prop.&& False", {
    val g = elements(proved,falsified,undecided)
    forAll(g)(p => (p && falsified) == falsified)
  })
  specify("Prop.&& Undecided", {
    val g = elements(proved,undecided)
    forAll(g)(p => (p && undecided) === undecided)
  })
  specify("Prop.&& Right prio", (sz: Int) => {
    val p = proved.addArg(Arg("","RHS",0)) && proved.addArg(Arg("","LHS",0))
    p(Gen.Params(sz,StdRand)) match {
      case Some(r) if r.args == Arg("","RHS",0)::Nil => true
      case _ => false
    }
  })

  specify("Prop.|| Commutativity", (p1: Prop, p2: Prop) =>
    (p1 || p2) === (p2 || p1)
  )
  specify("Prop.|| Exception", (p: Prop) =>
    (p || exception(null)) == exception(null)
  )
  specify("Prop.|| Identity", (p: Prop) =>
    (p || falsified) === p
  )
  specify("Prop.|| True", {
    val g = elements(proved,falsified,undecided)
    forAll(g)(p => (p || proved) == proved)
  })
  specify("Prop.|| Undecided", {
    val g = elements(falsified,undecided)
    forAll(g)(p => (p || undecided) === undecided)
  })

  specify("Prop.++ Commutativity", (p1: Prop, p2: Prop) =>
    (p1 ++ p2) === (p2 ++ p1)
  )
  specify("Prop.++ Exception", (p: Prop) =>
    (p ++ exception(null)) == exception(null)
  )
  specify("Prop.++ Identity 1", {
    val g = elements(falsified,proved,exception(null))
    forAll(g)(p => (p ++ proved) === p)
  })
  specify("Prop.++ Identity 2", (p: Prop) =>
    (p ++ undecided) === p
  )
  specify("Prop.++ False", {
    val g = elements(falsified,proved,undecided)
    forAll(g)(p => (p ++ falsified) === falsified)
  })


  // Types

  /** The result of evaluating a property */
  abstract sealed class Result(val args: List[Arg]) {
    override def equals(x: Any) = (this,x) match {
      case (_: True, _: True)   => true
      case (_: Proof, _: Proof)   => true
      case (_: False, _: False) => true
      case (_: Exception, _: Exception) => true
      case _ => false
    }

    def success = this match {
      case _:True => true
      case _:Proof => true
      case _ => false
    }

    def failure = this match {
      case _:False => true
      case _:Exception => true
      case _ => false
    }

    def addArg(a: Arg) = this match {
      case Proof(as) => Proof(a::as)
      case True(as) => True(a::as)
      case False(as) => False(a::as)
      case Exception(as,e) => Exception(a::as,e)
    }

  }

  /** The property was proved with the given arguments */
  sealed case class Proof(as: List[Arg]) extends Result(as)

  /** The property was true with the given arguments */
  sealed case class True(as: List[Arg]) extends Result(as)

  /** The property was false with the given arguments */
  sealed case class False(as: List[Arg]) extends Result(as)

  /** Evaluating the property with the given arguments raised an exception */
  sealed case class Exception(as: List[Arg], e: Throwable) extends Result(as)

  /** Boolean with support for implication */
  class ExtendedBoolean(b: Boolean) {
    /** Implication */
    def ==>(p: => Prop) = Prop.==>(b,p)
  }

  /** Any with support for implication and iff */
  class ExtendedAny[T](x: T) {
    /** Implication with several conditions */
    def imply(f: PartialFunction[T,Prop]) = Prop.imply(x,f)
    def iff(f: PartialFunction[T,Prop]) = Prop.iff(x,f)
  }


  // Implicit defs

  implicit def extendedBoolean(b: Boolean) = new ExtendedBoolean(b)

  implicit def extendedAny[T](x: T) = new ExtendedAny(x)

  implicit def propBoolean(b: Boolean): Prop = if(b) proved else falsified


  // Private support functions

  private def constantProp(r: Option[Result], descr: String) =
    new Prop(prms => r).label(descr)

  private implicit def genToProp(g: Gen[Result]) = new Prop(g.apply).label(g.label)

  private def provedToTrue(r: Result) = r match {
    case Proof(as) => True(as)
    case _ => r
  }


  // Property combinators

  /** A property that never is proved or falsified */
  lazy val undecided: Prop = constantProp(None, "undecided")
  specify("undecided", (prms: Gen.Params) => undecided(prms) == None)

  /** A property that always is false */
  lazy val falsified: Prop = constantProp(Some(False(Nil)), "falsified")
  specify("falsified", (prms: Gen.Params) => falsified(prms) iff {
    case Some(_: False) => true
  })

  /** A property that always is proved */
  lazy val proved: Prop = constantProp(Some(Proof(Nil)), "proved");
  specify("proved", (prms: Gen.Params) => proved(prms) iff {
    case Some(_: Proof) => true
  })

  /** A property that denotes an exception */
  def exception(e: Throwable) = constantProp(Some(Exception(Nil,e)),"exception")
  specify("exception",(prms:Gen.Params, e:Throwable) => exception(e)(prms) iff {
    case Some(Exception(_,f)) if f == e => true
  })

  /** A property that depends on the generator size */
  def sizedProp(f: Int => Prop): Prop = new Prop(prms => f(prms.size)(prms))

  /** Implication */
  def ==>(b: => Boolean, p: => Prop): Prop = property(if (b) p else undecided)

  /** Implication with several conditions */
  def imply[T](x: T, f: PartialFunction[T,Prop]): Prop =
    property(if(f.isDefinedAt(x)) f(x) else undecided)

  /** Property holds only if the given partial function is defined at
   *  <code>x</code>, and returns a property that holds */
  def iff[T](x: T, f: PartialFunction[T,Prop]): Prop =
    property(if(f.isDefinedAt(x)) f(x) else falsified)

  /** Combines properties into one, which is true if and only if all the
   *  properties are true */
  def all(ps: Iterable[Prop]) = new Prop(prms => 
    if(ps.forall(p => p(prms).getOrElse(False(Nil)).success)) proved(prms) 
    else falsified(prms)
  )
  specify("all", forAll(Gen.listOf1(value(proved)))(l => all(l))) 

  /** Combines properties into one, which is true if at least one of the
   *  properties is true */
  def atLeastOne(ps: Iterable[Prop]) = new Prop(prms => 
    if(ps.exists(p => p(prms).getOrElse(False(Nil)).success)) proved(prms) 
    else falsified(prms)
  )
  specify("atLeastOne", forAll(Gen.listOf1(value(proved)))(l => atLeastOne(l))) 

  /** Existential quantifier */
  def exists[A,P <% Prop](g: Gen[A])(f: A => P): Prop = for {
    a <- g
    r <- property(f(a))
    s <- r match {
           case _: True => proved
           case _: Proof => proved
           case _: False => undecided
           case Exception(_, e) => exception(e)
         }
  } yield s.addArg(Arg(g.label,a,0))

  /** Universal quantifier */
  def forAll[A,P <% Prop](g: Gen[A])(f: A => P): Prop = for {
    a <- g
    r <- property(f(a))
  } yield provedToTrue(r).addArg(Arg(g.label,a,0))

  /** Universal quantifier, shrinks failed arguments with given shrink
   *  function */
  def forAllShrink[A, P <% Prop](g: Gen[A],shrink: A => Stream[A])(f: A => P): Prop =
    new Prop((prms: Gen.Params) => {

      import Stream.{cons, empty}

      def getFirstFail(xs: Stream[A], shrinks: Int) = {
        val results = xs.map { x =>
          val p = property(f(x))
          p(prms).map(r => (x, provedToTrue(r).addArg(Arg(g.label,x,shrinks))))
        }
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
  def forAllDefaultShrink[T,P](g: Gen[T])(f: T => P)
    (implicit s: Shrink[T], p: P => Prop) = forAllShrink(g, shrink[T])(f)

  /** A property that holds if at least one of the given generators
   *  fails generating a value */
  def someFailing[T](gs: Iterable[Gen[T]]) = atLeastOne(gs.map(_ === fail))

  /** A property that holds iff none of the given generators
   *  fails generating a value */
  def noneFailing[T](gs: Iterable[Gen[T]]) = all(gs.map(_ !== fail))

  /** Wraps and protects a property */
  def property[P <% Prop](p: => P): Prop = new Prop(prms =>
    (try { p: Prop } catch { case e => exception(e) })(prms)
  )

  /** Converts a function into a property */
  def property[A1,P] (
    f:  A1 => P)(implicit
    p: P => Prop,
    a1: Arbitrary[A1], s1: Shrink[A1]
  ) = forAllShrink(arbitrary[A1],shrink[A1])(f andThen p)

  /** Converts a function into a property */
  def property[A1,A2,P] (
    f:  (A1,A2) => P)(implicit
    p: P => Prop,
    a1: Arbitrary[A1], s1: Shrink[A1],
    a2: Arbitrary[A2], s2: Shrink[A2]
  ): Prop = property((a: A1) => property(f(a, _:A2)))

  /** Converts a function into a property */
  def property[A1,A2,A3,P] (
    f:  (A1,A2,A3) => P)(implicit
    p: P => Prop,
    a1: Arbitrary[A1], s1: Shrink[A1],
    a2: Arbitrary[A2], s2: Shrink[A2],
    a3: Arbitrary[A3], s3: Shrink[A3]
  ): Prop = property((a: A1) => property(f(a, _:A2, _:A3)))

  /** Converts a function into a property */
  def property[A1,A2,A3,A4,P] (
    f:  (A1,A2,A3,A4) => P)(implicit
    p: P => Prop,
    a1: Arbitrary[A1], s1: Shrink[A1],
    a2: Arbitrary[A2], s2: Shrink[A2],
    a3: Arbitrary[A3], s3: Shrink[A3],
    a4: Arbitrary[A4], s4: Shrink[A4]
  ): Prop = property((a: A1) => property(f(a, _:A2, _:A3, _:A4)))

  /** Converts a function into a property */
  def property[A1,A2,A3,A4,A5,P] (
    f:  (A1,A2,A3,A4,A5) => P)(implicit
    p: P => Prop,
    a1: Arbitrary[A1], s1: Shrink[A1],
    a2: Arbitrary[A2], s2: Shrink[A2],
    a3: Arbitrary[A3], s3: Shrink[A3],
    a4: Arbitrary[A4], s4: Shrink[A4],
    a5: Arbitrary[A5], s5: Shrink[A5]
  ): Prop = property((a: A1) => property(f(a, _:A2, _:A3, _:A4, _:A5)))

  /** Converts a function into a property */
  def property[A1,A2,A3,A4,A5,A6,P] (
    f:  (A1,A2,A3,A4,A5,A6) => P)(implicit
    p: P => Prop,
    a1: Arbitrary[A1], s1: Shrink[A1],
    a2: Arbitrary[A2], s2: Shrink[A2],
    a3: Arbitrary[A3], s3: Shrink[A3],
    a4: Arbitrary[A4], s4: Shrink[A4],
    a5: Arbitrary[A5], s5: Shrink[A5],
    a6: Arbitrary[A6], s6: Shrink[A6]
  ): Prop = property((a: A1) => property(f(a, _:A2, _:A3, _:A4, _:A5, _:A6)))

}
