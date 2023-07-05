/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package org.scalacheck

import Prop.propBoolean

object PropertyFilterSampleSpecification extends Properties("PropertyFilterSample") {

  property("positive numbers") = Prop.forAll(Gen.posNum[Int]) { n => n > 0 }

  property("negative numbers") = Prop.forAll(Gen.negNum[Int]) { n => n < 0 }

  property("lowercase alpha characters") = Prop.forAll(Gen.alphaLowerChar) { c =>
    c.toInt >= 97 && c.toInt <= 122
  }
}

object PropertyFilterSpecification extends Properties("PropertyFilter") {

  val nl = System.lineSeparator

  private def diff(filter: Option[String], actual: collection.Seq[String], expected: collection.Seq[String]): String = {
    s"filter: ${filter.getOrElse("not supplied")}" +
      s"${nl}expected values:$nl" +
      s"\t${expected.mkString(s"$nl\t")}" +
      s"${nl}actual values:$nl" +
      s"\t${actual.mkString(s"$nl\t")}"
  }

  private def prop(
      filter: Option[String],
      actualNames: collection.Seq[String],
      expectedNames: collection.Seq[String]
  ): Prop = {
    def lengthProp = actualNames.length == expectedNames.length

    def props = actualNames.forall(expectedNames.contains)

    (lengthProp && props).labelImpl2(diff(filter, actualNames, expectedNames))
  }

  property("filter properties by predicate") =
    Prop.forAllNoShrink(
      Gen.option(
        Gen.oneOf(
          "PropertyFilterSample.*numbers",
          "PropertyFilterSample.*alpha"))) { pf =>
      val testParams =
        Test.Parameters.default.withPropFilter(pf)

      val props =
        Test.checkProperties(
          testParams,
          PropertyFilterSampleSpecification
        )

      val propNames = props.map(_._1)

      if (pf.exists(_.contains("*numbers"))) {
        val expected =
          Seq(
            "PropertyFilterSample.positive numbers",
            "PropertyFilterSample.negative numbers"
          )

        prop(pf, propNames, expected)
      } else if (pf.exists(_.contains("*alpha"))) {
        val expected = Seq("PropertyFilterSample.lowercase alpha characters")

        prop(pf, propNames, expected)
      } else { // no filter
        val expected = Seq(
          "PropertyFilterSample.positive numbers",
          "PropertyFilterSample.negative numbers",
          "PropertyFilterSample.lowercase alpha characters"
        )

        prop(pf, propNames, expected)
      }
    }
}
