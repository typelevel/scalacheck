package example

import org.scalacheck._

object ScalaJSExampleSpec extends Properties("ScalaCheck-scalajs") {

  property("dummy") = Prop.forAll { l: List[String] => l.reverse.reverse == l }

}
