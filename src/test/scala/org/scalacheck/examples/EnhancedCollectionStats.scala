package org.scalacheck

import org.scalacheck.Test.Parameters
import org.scalacheck.Prop._




object EnhancedCollectionStats extends org.scalacheck.Properties("AreasIsGreaterThan0") with ExtendedCommandLineRunner {

  override def extendParams(params: Parameters): Parameters = {
    val newCallBack = params.testCallback.chain(
      new StatisticsCollector{
      override def failOnNotANumber: Boolean = false
    })
    params.withTestCallback(newCallBack)
  }


  /** The logic for main, separated out to make it easier to
    * avoid System.exit calls.  Returns exit code.
    */
  override def mainRunner(args: Array[String]): Int = customRun(args)

  trait Figure{
    def area:Double
  }

  case class Square(side:Double) extends Figure {
    override def area: Double = side * side
  }

  case class Circle(radius:Double) extends Figure {

    override def area: Double = radius * radius * Math.PI

  }

  val ranges = Vector.tabulate(10){ index => (index *20d, (index +1) * 20d)}

  val classifier = (area:Double) => ranges.find {
    case (min,max) => area >= min && area <= max
  }.map{
    case(min,max) => s"Figures with area in [$min,$max]"
  }.getOrElse("Others")

  val circleGen = for { validSide <- Gen.choose(0D,10D)} yield new Circle(validSide)

  val rectangleGen = for { validRadius <- Gen.choose(0D,10D)} yield new Square(validRadius)

  implicit val arbitraryFigure = Arbitrary[Figure]{ Gen oneOf (circleGen, rectangleGen)}


  property("areaIsAlwaysBiggerThan0") = forAll { figure: Figure =>
    collect(figure.area){
      figure.area >= 0
    }
  }

}
