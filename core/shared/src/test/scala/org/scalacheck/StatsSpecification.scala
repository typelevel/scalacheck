/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package org.scalacheck

import Prop.{forAllNoShrink => forAll}

object StatsSpecification extends Properties("Stats") {

  // each test run generates 5k samples, so only do 10 of them.
  override def overrideParameters(ps: Test.Parameters): Test.Parameters =
    ps.withMinSuccessfulTests(10)

  // we sample the distribution 5000 times, and expect the mean and
  // standard deviation to be within ±10% of the true value.
  val Samples = 5000
  val ErrorRate = 0.1

  // we'll generate relatively small, well-behaved mean values.
  val genMean = Gen.choose(10.0, 20.0)

  // generate a number of trials for use with binomial
  val genTrials = Gen.choose(10, 30)

  // generate a probability value
  val genP = Gen.choose(0.2, 0.8)

  property("prob") =
    forAll(genP) { p =>
      val gen = Gen.prob(p).map(b => if (b) 1.0 else 0.0)
      check(gen, mean = p, stdDev = Math.sqrt(p * (1.0 - p)))
    }

  property("gaussian") =
    forAll(genMean, genMean) { (mean, stdDev) =>
      val gen = Gen.gaussian(mean, stdDev)
      check(gen, mean, stdDev)
    }

  property("exponential") =
    forAll(genMean) { mean =>
      val gen = Gen.exponential(1.0 / mean)
      check(gen, mean = mean, stdDev = mean)
    }

  property("geometric") =
    forAll(genMean) { mean =>
      val gen = Gen.geometric(mean).map(_.toDouble)
      val p = 1.0 / (mean + 1.0)
      val stdDev = Math.sqrt((1.0 - p) / (p * p))
      check(gen, mean, stdDev)
    }

  property("poisson") =
    forAll(genMean) { rate =>
      val gen = Gen.poisson(rate).map(_.toDouble)
      check(gen, mean = rate, stdDev = Math.sqrt(rate))
    }

  property("binomial") =
    forAll(genTrials, genP) { (trials, p) =>
      val gen = Gen.binomial(Gen.prob(p), trials).map(_.toDouble)
      val mean = trials * p
      val stdDev = Math.sqrt(trials * p * (1.0 - p))
      check(gen, mean, stdDev)
    }

  def check(gen: Gen[Double], mean: Double, stdDev: Double): Prop = {
    val (e1, e2) = (mean * ErrorRate, stdDev * ErrorRate)
    val (μ, σ) = computeStats(gen, Samples)
    (mean ± e1).contains(μ) && (stdDev ± e2).contains(σ)
  }

  def computeStats(g: Gen[Double], samples: Int): (Double, Double) = {
    val vg = Gen.buildableOfN[Vector[Double], Double](samples, g)
    val xs = vg.sample.get
    val mean = xs.sum / xs.size
    val stdDev = Math.sqrt(xs.iterator.map(x => Math.pow(x - mean, 2)).sum / xs.size)
    (mean, stdDev)
  }

  case class Bounds(min: Double, max: Double) {
    def contains(x: Double): Prop =
      Prop(min <= x && x <= max).labelImpl2(s"($min <= $x <= $max) was false")
  }

  implicit class MakeBounds(val n: Double) extends AnyVal {
    def ±(error: Double): Bounds = Bounds(n - error, n + error)
  }
}
