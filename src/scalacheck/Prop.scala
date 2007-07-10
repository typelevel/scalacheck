package scalacheck

/** A property is a generator that generates a property result */
abstract class Prop extends Gen[Prop.Result]

object Prop {

  import Gen.{value, fail, arbitrary}

  // Types

  /** The result of evaluating a property */
  abstract sealed class Result(val args: List[String])

  /** The property was true with the given arguments */
  case class PropTrue(as: List[String]) extends Result(as)

  /** The property was false with the given arguments */
  case class PropFalse(as: List[String]) extends Result(as)

  /** Evaluating the property with the given arguments raised an exception */
  case class PropException(e: Throwable, as: List[String]) extends Result(as)



  // Private support functions

  private implicit def genToProp(g: Gen[Result]) = new Prop {
    def apply(prms: Gen.Params) = g(prms)
  }

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

  /** A property that never is proved or falsified */
  def rejected = fail

  /** A property that always is false */
  def falsified = value(PropFalse(Nil))

  /** A property that always is true */
  def proved = value(PropTrue(Nil))

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

  implicit def propBoolean(b: Boolean): Prop = 
    value(if(b) PropTrue(Nil) else PropFalse(Nil)) 

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
