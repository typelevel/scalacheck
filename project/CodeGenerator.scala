object CodeGenerator {

  private val f = (n: Int) => {
    val xs = 1 to n
    val tparams = xs.map("A" + _)
    s"""
  final def fromFunction$n[${tparams.mkString(", ")}, Z](f: (${tparams.mkString(", ")}) => Z)(implicit ${tparams.map(t => s"$t: Arbitrary[$t]").mkString(", ")}): Arbitrary[Z] =
    Arbitrary(for{
      ${xs.map(i => s"a$i <- A$i.arbitrary").mkString("; ")}
    }yield f(${xs.map("a" + _).mkString(", ")}))

  final def fromFunction[${tparams.mkString(", ")}, Z](f: (${tparams.mkString(", ")}) => Z)(implicit ${tparams.map(t => s"$t: Arbitrary[$t]").mkString(", ")}): Arbitrary[Z] =
    fromFunction$n(f)(${tparams.mkString(", ")})"""
  }

  val code = s"""package org.scalacheck

abstract class ArbitraryFromFunction {

  ${(1 to 22).map(f).mkString("\n")}

}
"""

}
