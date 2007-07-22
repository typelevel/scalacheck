package scalacheck

/** A property is a generator that generates a property result */
abstract class Prop extends Gen[Prop.Result] {

  import Prop.{True,False,Exception}

  /** Returns a new property that holds if and only if both this
   *  and the given property hold. If one of the properties doesn't
   *  generate a result, the new property will generate false.
   */
  def &&(p: Prop): Prop = combine(p) {
    case (x, Some(True(_,_))) => x
    case (Some(True(_,_)), x) => x

    case (x@Some(False(_,_)), _) => x
    case (_, x@Some(False(_,_))) => x

    case (None, x) => x
    case (x, None) => x

    case (x, _) => x
  }

  /** Returns a new property that holds if either this
   *  or the given property (or both) hold.
   */
  def ||(p: Prop): Prop = combine(p) {
    case (x@Some(True(_,_)), _) => x
    case (_, x@Some(True(_,_))) => x

    case (Some(False(_,_)), x) => x
    case (x, Some(False(_,_))) => x

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

    case (x, Some(True(_,_))) => x
    case (Some(True(_,_)), x) => x

    case (x@Some(False(_,_)), _) => x
    case (_, x@Some(False(_,_))) => x

    case (x, _) => x
  }

  /** Returns a new property that holds if and only if both this
   *  and the given property generates the same result.
   */
  def ==(p: Prop): Prop = new Prop {
    def apply(prms: Gen.Params) = (Prop.this(prms), p(prms)) match {
      case (None,None) => Prop.proved(prms)
      case (Some(r1),Some(r2)) if r1 == r2 => Prop.proved(prms)
      case _ => Prop.falsified(prms)
    }
  }

  def addArg(arg: String) = new Prop {
    override def toString = Prop.this.toString
    def apply(prms: Gen.Params) = Prop.this(prms) match {
      case None => None
      case Some(r) => Some(r.addArg(arg))
    }
  }

  def shrinked = new Prop {
    override def toString = Prop.this.toString
    def apply(prms: Gen.Params) = Prop.this(prms) match {
      case None => None
      case Some(r) => Some(r.shrinked)
    }
  }

}

object Prop extends Testable {

  import Gen.{value, fail, frequency, elements}
  import Arbitrary._

  // Properties for the Prop class

  addProperty("Prop.Prop.&& Commutativity",
              (p1: Prop, p2: Prop) => (p1 && p2) == (p2 && p1))
  addProperty("Prop.Prop.&& Identity",
              (p: Prop) => (p && proved) == p)
  addProperty("Prop.Prop.&& False",
              (p: Prop) => (p && falsified) == falsified)
  addProperty("Prop.Prop.&& Right prio", (sz: Int) => {
    val p = proved.addArg("RHS") && proved.addArg("LHS")
    p(Gen.Params(sz,StdRand)) match {
      case Some(r) if r.args == "RHS"::Nil => true
      case _ => false
    }
  })

  addProperty("Prop.Prop.|| Commutativity",
              (p1: Prop, p2: Prop) => (p1 || p2) == (p2 || p1))
  addProperty("Prop.Prop.|| Identity",
              (p: Prop) => (p || falsified) == p)
  addProperty("Prop.Prop.|| True",
              (p: Prop) => (p || proved) == proved)

  addProperty("Prop.Prop.++ Commutativity",
              (p1: Prop, p2: Prop) => (p1 ++ p2) == (p2 ++ p1))
  addProperty("Prop.Prop.++ Identity",
              (p: Prop) => (p ++ rejected) == p)
  addProperty("Prop.Prop.++ False",
              (p: Prop) => (p ++ falsified) == falsified)
  addProperty("Prop.Prop.++ True",
              forAll(elements(List(proved,falsified,exception(null))))
                    (p => (p ++ proved) == p))



  // Types

  /** The result of evaluating a property */
  abstract sealed class Result(val args: List[String], val shrinks: Int) {
    override def equals(x: Any) = (this,x) match {
      case (True(_,_),True(_,_))   => true
      case (False(_,_),False(_,_)) => true
      case (Exception(_,_,_),Exception(_,_,_)) => true
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

    def addArg(a: String) = this match {
      case True(as,s) => True(a::as,s)
      case False(as,s) => False(a::as,s)
      case Exception(as,s,e) => Exception(a::as,s,e)
    }

    def shrinked = this match {
      case True(as,s) => True(as,s+1)
      case False(as,s) => False(as,s+1)
      case Exception(as,s,e) => Exception(as,s+1,e)
    }

  }

  /** The property was true with the given arguments */
  case class True(as: List[String], ss: Int) extends Result(as,ss)

  /** The property was false with the given arguments */
  case class False(as: List[String],ss: Int) extends Result(as,ss)

  /** Evaluating the property with the given arguments raised an exception */
  case class Exception(as: List[String], ss: Int, e: Throwable) 
    extends Result(as,ss)



  // Private support functions

  private def constantProp(r: Option[Result], descr: String) = new Prop {
    override def toString = descr
    def apply(prms: Gen.Params) = r
  }

  private implicit def genToProp(g: Gen[Result]) = new Prop {
    def apply(prms: Gen.Params) = g(prms)
  }


  // Property combinators

  /** A property that never is proved or falsified */
  def rejected: Prop = constantProp(None, "Prop.rejected")

  /** A property that always is false */
  def falsified: Prop = constantProp(Some(False(Nil,0)), "Prop.falsified")

  /** A property that always is true */
  def proved: Prop = constantProp(Some(True(Nil,0)), "Prop.proved");

  /** A property that denotes an exception */
  def exception(e: Throwable): Prop =
    constantProp(Some(Exception(Nil,0,e)), "Prop.exception")

  /** Implication */
  def ==>(b: Boolean, p: => Prop): Prop =
    property(() => if (b) p else rejected)

  /** Implication with several conditions */
  def imply[T](x: T, f: PartialFunction[T,Prop]): Prop =
    property(() => if(f.isDefinedAt(x)) f(x) else rejected)

  /** Universal quantifier */
  def forAll[A,P](g: Gen[A])(f: A => P)(implicit p: P => Prop): Prop = for {
    a <- g
    r <- try { p(f(a)) } catch { case e => exception(e) }
  } yield r.addArg(a.toString)

  /** Universal quantifier, shrinks failed arguments */
  def forAllShrink[A](g: Gen[A], shrink: A => Seq[A])(f: A => Prop): Prop = for {
    a <- g
    prop = forAll(g)(f)
    r1 <- prop
    r2 <- if(r1.failure) 
            forAllShrink(elements(shrink(a)), shrink)(f).shrinked && prop
          else prop
  } yield r2

  /** Combines properties into one, which is true if and only if all the
   *  properties are true
   */
  def all(ps: Seq[Prop]) = (ps :\ proved) (_ && _)

  class ExtendedBoolean(b: Boolean) {
    /** Implication */
    def ==>(p: => Prop) = Prop.==>(b,p)
  }

  class ExtendedAny[T](x: T) {
    /** Implication with several conditions */
    def imply(f: PartialFunction[T,Prop]) = Prop.imply(x,f)
  }



  // Implicit defs

  implicit def extendedBoolean(b: Boolean) = new ExtendedBoolean(b)
  implicit def extendedAny[T](x: T) = new ExtendedAny(x)



  // Implicit properties for common types

  implicit def propBoolean(b: Boolean): Prop = if(b) proved else falsified

  def property[P] (
    f: () => P)(implicit
    p: P => Prop
  ): Prop = new Prop {
    def apply(prms: Gen.Params) = 
      (try { p(f()) } catch { case e => exception(e) })(prms)
  }

  def property[A1,P] (
    f:  A1 => P)(implicit 
    p:  P => Prop, 
    a1: Arb[A1] => Arbitrary[A1]
  ) = forAll(arbitrary[A1])(f)

  def property[A1,A2,P] (
    f:  (A1,A2) => P)(implicit
    p:  P => Prop,
    a1: Arb[A1] => Arbitrary[A1],
    a2: Arb[A2] => Arbitrary[A2]
  ): Prop = for {
    a <- arbitrary[A1]
    r <- property(f(a, _:A2))
  } yield r.addArg(a.toString)

  def property[A1,A2,A3,P] (
    f:  (A1,A2,A3) => P)(implicit
    p:  P => Prop,
    a1: Arb[A1] => Arbitrary[A1],
    a2: Arb[A2] => Arbitrary[A2],
    a3: Arb[A3] => Arbitrary[A3]
  ): Prop = for {
    a <- arbitrary[A1]
    r <- property(f(a, _:A2, _:A3))
  } yield r.addArg(a.toString)

  def property[A1,A2,A3,A4,P] (
    f:  (A1,A2,A3,A4) => P)(implicit
    p:  P => Prop,
    a1: Arb[A1] => Arbitrary[A1],
    a2: Arb[A2] => Arbitrary[A2],
    a3: Arb[A3] => Arbitrary[A3],
    a4: Arb[A4] => Arbitrary[A4]
  ): Prop = for {
    a <- arbitrary[A1]
    r <- property(f(a, _:A2, _:A3, _:A4))
  } yield r.addArg(a.toString)

  def property[A1,A2,A3,A4,A5,P] (
    f:  (A1,A2,A3,A4,A5) => P)(implicit
    p:  P => Prop,
    a1: Arb[A1] => Arbitrary[A1],
    a2: Arb[A2] => Arbitrary[A2],
    a3: Arb[A3] => Arbitrary[A3],
    a4: Arb[A4] => Arbitrary[A4],
    a5: Arb[A5] => Arbitrary[A5]
  ): Prop = for {
    a <- arbitrary[A1]
    r <- property(f(a, _:A2, _:A3, _:A4, _:A5))
  } yield r.addArg(a.toString)

  def property[A1,A2,A3,A4,A5,A6,P] (
    f:  (A1,A2,A3,A4,A5,A6) => P)(implicit
    p:  P => Prop,
    a1: Arb[A1] => Arbitrary[A1],
    a2: Arb[A2] => Arbitrary[A2],
    a3: Arb[A3] => Arbitrary[A3],
    a4: Arb[A4] => Arbitrary[A4],
    a5: Arb[A5] => Arbitrary[A5],
    a6: Arb[A6] => Arbitrary[A6]
  ): Prop = for {
    a <- arbitrary[A1]
    r <- property(f(a, _:A2, _:A3, _:A4, _:A5, _:A6))
  } yield r.addArg(a.toString)

}
