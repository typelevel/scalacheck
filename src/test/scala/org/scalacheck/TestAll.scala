package org.scalacheck

object TestAll {
  object ScalaCheckSpecification extends Properties("ScalaCheck") {
    include(ArbitrarySpecification)
    include(GenSpecification)
    include(PropSpecification)
    include(TestSpecification)
    include(CommandsSpecification)
    include(commands.CommandsSpecification)
  }

  def main(args: Array[String]) = ScalaCheckSpecification.main(args)
}
