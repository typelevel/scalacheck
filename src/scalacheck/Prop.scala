package scalacheck

/** A property is a generator that generates a property result */
abstract class Prop extends Gen[Prop.Result] {

  import Prop.{True,False,Exception}

  def &&(p: Prop): Prop = combine(p) {
    case (Some(True(_)), x) => x
    case (x, Some(True(_))) => x

    case (x@Some(False(_)), _) => x
    case (_, x@Some(False(_))) => x

    case (None, x) => x
    case (x, None) => x

    case (x, _) => x
  }

  def ||(p: Prop): Prop = combine(p) {
    case (x@Some(True(_)), _) => x
    case (_, x@Some(True(_))) => x

    case (Some(False(_)), x) => x
    case (x, Some(False(_))) => x

    case (None, x) => x
    case (x, None) => x

    case (x, _) => x
  }

  def ++(p: Prop): Prop = combine(p) {
    case (None, x) => x
    case (x, None) => x

    case (Some(True(_)), x) => x
    case (x, Some(True(_))) => x

    case (x@Some(False(_)), _) => x
    case (_, x@Some(False(_))) => x

    case (x, _) => x
  }

  def ==(p: Prop): Prop = new Prop {
    def apply(prms: Gen.Params) = (Prop.this(prms), p(prms)) match {
      case (None,None) => Prop.proved(prms)
      case (Some(r1),Some(r2)) if r1 == r2 => Prop.proved(prms)
      case _ => Prop.falsified(prms)
    }
  }

  def addArgs(as: List[String]) = new Prop {
    override def toString = Prop.this.toString
    def apply(prms: Gen.Params) = Prop.this(prms) match {
      case None => None
      case Some(True(as2))        => Some(True(as ::: as2))
      case Some(False(as2))       => Some(False(as ::: as2))
      case Some(Exception(e,as2)) => Some(Exception(e,as ::: as2))
    }
  }

}

object Prop extends Testable {

  import Gen.{value, fail, arbitrary, frequency, elements}

  // Properties for the Prop class

  addProperty("Prop.Prop.&& Commutativity",
              (p1: Prop, p2: Prop) => (p1 && p2) == (p2 && p1))
  addProperty("Prop.Prop.&& Identity",
              (p: Prop) => (p && proved) == p)
  addProperty("Prop.Prop.&& False",
              (p: Prop) => (p && falsified) == falsified)

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
  abstract sealed class Result(val args: List[String]) {
    override def equals(x: Any) = (this,x) match {
      case (True(_),True(_))   => true
      case (False(_),False(_)) => true
      case (Exception(_,_),Exception(_,_)) => true
      case _ => false
    }
  }

  /** The property was true with the given arguments */
  case class True(as: List[String]) extends Result(as)

  /** The property was false with the given arguments */
  case class False(as: List[String]) extends Result(as)

  /** Evaluating the property with the given arguments raised an exception */
  case class Exception(e: Throwable, as: List[String]) extends Result(as)


  /** Generates an arbitrary prop */
  implicit def arbitraryProp(x: Arbitrary[Prop]): Gen[Prop] = frequency(
    List(
      (5, value(proved)),
      (4, value(falsified)),
      (2, value(rejected)),
      (1, value(exception(null)))
    )
  )


  // Private support functions

  private def genToProp(g: Gen[Result], descr: String) = new Prop {
    override def toString = descr
    def apply(prms: Gen.Params) = g(prms)
  }

  private implicit def genToProp(g: Gen[Result]) = new Prop {
    def apply(prms: Gen.Params) = g(prms)
  }

  private def mkProp(p: => Prop, as: Any*): Prop = {
    val pr = try { p } catch { case e => exception(e) }
    pr.addArgs(as.map(_.toString).toList)
  }



  // Property combinators

  /** A property that never is proved or falsified */
  def rejected: Prop = genToProp(fail, "Prop.rejected")

  /** A property that always is false */
  def falsified: Prop = genToProp(value(False(Nil)), "Prop.falsified")

  /** A property that always is true */
  def proved: Prop = genToProp(value(True(Nil)), "Prop.proved");

  /** A property that denotes an exception */
  def exception(e: Throwable): Prop =
    genToProp(value(Exception(e,Nil)), "Prop.exception")

  /** Implication */
  def ==>(b: Boolean, p: => Prop): Prop =
    property(() => if (b) p else rejected)

  /** Implication with several conditions */
  def imply[T](x: T, f: PartialFunction[T,Prop]): Prop =
    property(() => if(f.isDefinedAt(x)) f(x) else rejected)

  /** Universal quantifier */
  def forAll[T](g: Gen[T])(f: T => Prop): Prop = for {
    t <- g
    r <- mkProp(f(t), t)
  } yield r

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

  def property[P]
    (f:  () => P)(implicit
     p:  P => Prop): Prop = for
  {
    x <- proved // to keep from evaluating f immediately
    r <- mkProp(p(f()))
  } yield r

  def property[A1,P]
    (f:  A1 => P)(implicit
     p:  P => Prop,
     g1: Arbitrary[A1] => Gen[A1]): Prop = for
  {
    a1 <- g1(arbitrary)
    r  <- mkProp(p(f(a1)), a1)
  } yield r

  def property[A1,A2,P]
    (f:  (A1,A2) => P)(implicit
     p:  P => Prop,
     g1: Arbitrary[A1] => Gen[A1],
     g2: Arbitrary[A2] => Gen[A2]): Prop = for
  {
    a1 <- g1(arbitrary)
    a2 <- g2(arbitrary)
    r  <- mkProp(p(f(a1,a2)),a1,a2)
  } yield r

  def property[A1,A2,A3,P]
    (f:  (A1,A2,A3) => P)(implicit
     p:  P => Prop,
     g1: Arbitrary[A1] => Gen[A1],
     g2: Arbitrary[A2] => Gen[A2],
     g3: Arbitrary[A3] => Gen[A3]): Prop = for
  {
    a1 <- g1(arbitrary)
    a2 <- g2(arbitrary)
    a3 <- g3(arbitrary)
    r  <- mkProp(p(f(a1,a2,a3)),a1,a2,a3)
  } yield r

  def property[A1,A2,A3,A4,P]
    (f:  (A1,A2,A3,A4) => P)(implicit
     p:  P => Prop,
     g1: Arbitrary[A1] => Gen[A1],
     g2: Arbitrary[A2] => Gen[A2],
     g3: Arbitrary[A2] => Gen[A3],
     g4: Arbitrary[A3] => Gen[A4]): Prop = for
  {
    a1 <- g1(arbitrary)
    a2 <- g2(arbitrary)
    a3 <- g3(arbitrary)
    a4 <- g4(arbitrary)
    r  <- mkProp(p(f(a1,a2,a3,a4)),a1,a2,a3,a4)
  } yield r

}
