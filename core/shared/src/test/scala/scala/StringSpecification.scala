/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package scala

import org.scalacheck.Gen
import org.scalacheck.Prop
import org.scalacheck.Properties

object StringSpecification extends Properties("scala.String") {

  property("mkString") = { // Issue #721
    import scala.collection.JavaConverters._
    val listOfGens: List[Gen[Char]] = "".toList.map(Gen.const(_))
    val g1: Gen[String] = Gen.sequence(listOfGens).map(_.asScala.mkString)
    val g2: Gen[String] = Gen.sequence(List(Gen.listOf(' ').map(_.mkString))).map(_.asScala.mkString)
    Prop.forAll(g1, g2) { (_, _) =>
      true
    }
  }
}
