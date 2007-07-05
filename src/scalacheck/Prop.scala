package scalacheck

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

  def imply[T](x: T, f: PartialFunction[T,Prop]) = 
    if(f.isDefinedAt(x)) f(x) else rejected

  def forAll[T](g: Gen[T])(f: T => Prop): Prop = for {
    t <- g
    r <- mkProp(f(t), t)
  } yield r


  // Convenience functions

  implicit def extBoolean(b: Boolean) = new ExtBoolean(b)
  class ExtBoolean(b: Boolean) {
    def ==>(p: => Prop) = Prop.==>(b,p)
  }

  implicit def extAny[T](x: T) = new ExtAny(x)
  class ExtAny[T](x: T) {
    def imply(f: PartialFunction[T,Prop]) = Prop.imply(x,f)
  }


  // Implicit properties for common types

  implicit def propBoolean(b: Boolean) =
    value(if(b) PropTrue(Nil) else PropFalse(Nil))

  def property[A1,P]
    (f:  A1 => P)(implicit
     p:  P => Prop,
     g1: Arbitrary[A1] => Gen[A1]) = for
  {
    a1 <- g1(arbitrary)
    r  <- mkProp(p(f(a1)), a1)
  } yield r

  def property[A1,A2,P]
    (f:  (A1,A2) => P)(implicit
     p:  P => Prop,
     g1: Arbitrary[A1] => Gen[A1],
     g2: Arbitrary[A2] => Gen[A2]) = for
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
     g3: Arbitrary[A3] => Gen[A3]) = for
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
     g4: Arbitrary[A3] => Gen[A4]) = for
  {
    a1 <- g1(arbitrary)
    a2 <- g2(arbitrary)
    a3 <- g3(arbitrary)
    a4 <- g4(arbitrary)
    r  <- mkProp(p(f(a1,a2,a3,a4)),a1,a2,a3,a4)
  } yield r

}
