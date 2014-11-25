// TEMPORARY TEST FILE //

package org.scalacheck

object Main {

  def main(args: Array[String]) = {

    val p = Prop.forAll { l: List[Int] => l.reverse.reverse == l }

    println("Running on JVM")

    p.check
  }

}
