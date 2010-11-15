package org.scalacheck

object TestAll {
  object ScalaCheckSpecification extends Properties("ScalaCheck") {
    include(GenSpecification)
    include(PropSpecification)
    include(TestSpecification)
  }

  def main(args: Array[String]) = ScalaCheckSpecification.main(args)
}
