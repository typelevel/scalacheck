// TEMPORARY TEST FILE //

package org.scalacheck

import scala.scalajs.js

object Main extends js.JSApp {

  def main() = {

    val p = Prop.forAll { l: List[Int] => l.reverse.reverse == l }

    println("Running on JS")

    p.check
  }

}
