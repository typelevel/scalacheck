package org.scalacheck

import org.scalacheck.Prop.{forAll, BooleanOperators,classify}

/**
 * Created with IntelliJ IDEA.
 * Author: Edmondo Porcu
 * Date: 27/02/14
 * Time: 17:21
 *
 */
object EnhancedCollection extends org.scalacheck.Properties("AreasIsGreaterThan0") {


  trait Figure{
    def area:Double
  }

  case class Square(side:Double) extends Figure {
    override def area: Double = side * side
  }

  case class Circle(radius:Double) extends Figure {

    override def area: Double = radius * radius * Math.PI

  }

  val ranges = Vector.tabulate(10){ index => (index.toDouble *20d, (index +1) * 20d)}

  val classifier = (area:Double) => ranges.find {
    case (min,max) => area >= min && area <= max
  }.map{
    case(min,max) => s"Figures with area in [$min,$max]"
  }.getOrElse("Others")

  val circleGen = for { validSide <- Gen.choose(0D,10D)} yield new Square(validSide)

  val rectangleGen = for { validRadius <- Gen.choose(0D,10D)} yield new Circle(validRadius)

  implicit val arbitraryFigure = Arbitrary[Figure]{ Gen oneOf (circleGen, rectangleGen)}


  property("areaIsAlwaysBiggerThan0") = forAll {
    figure: Figure =>
      classify(figure.area, classifier){
        figure.area >= 0
      }
  }

}
