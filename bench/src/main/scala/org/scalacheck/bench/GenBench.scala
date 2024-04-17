/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package org.scalacheck.bench

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.rng.Seed

/** Generator benchmarks
 *
 *  Since generators may run very quickly (or very slowly) depending on the seed and size parameter used, we want to
 *  make sure these are held constant across runs. Otherwise, we might believe a particular change made a given
 *  generator faster (or slower) when in fact we just got lucky (or unlucky).
 *
 *  We use `seedCount` to choose how many seeds we will benchmark with. For each seed, we run the given generator. So if
 *  `seedCount` is 100, the average time we get is the time to generate values for the 100 seeds we started with.
 *
 *  The size parameter also plays a major role in how long generators take. ScalaCheck's default parameters start with
 *  100. You can benchmark additional size parameters to see how changes to this parameter effect generator run time.
 *
 *  Finally, a generator's `apply` method may fail to generate a value. This might allow a slow generator that fails 50%
 *  of the time to appear faster than other generators that don't fail. Since we expect all generators to be able to
 *  succeed, we benchmark using `pureApply`, which will retry up to 100 times before crashing. This way, there's no
 *  chance that a flaky generator gets an advantage during benchmarking.
 *
 *  In theory, we should do as much work as possible outside the benchmark methods to avoid benchmarking construction
 *  time. However, since many generators are constructed inside of `flatMap` calls during generation we do want these
 *  constructors to also run as quickly as possible. For that reason, we mostly choose to assemble our generators inside
 *  the benchmark methods.
 *
 *  Prefer to add new benchmarks instead of modifying existing ones. This will enable us to compare the results for a
 *  given benchmark over time.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class GenBench {

  // number of seeds to use
  @Param(Array("100"))
  var seedCount: Int = 0

  // size parameter to use
  // Gen.Parameters.default uses 100
  @Param(Array("100"))
  var genSize: Int = 0

  // will be initialized in setup()
  var seeds: List[Seed] = Nil
  var params: Gen.Parameters = null

  @Setup
  def setup(): Unit = {
    // this guarantees a deterministic set of seeds for any given
    // seedCount, which helps ensure we're making fair comparisons
    // when we benchmark several different branches.
    val start: Long = 993423L
    val preseeds = start until (start + seedCount)
    seeds = preseeds.map(n => Seed(n)).toList

    val p = Gen.Parameters.default
    params = if (genSize <= 0) p else p.withSize(genSize)
  }

  val genInt: Gen[Int] =
    Gen.choose(Int.MinValue, Int.MaxValue)

  @Benchmark
  def const_(): List[Long] =
    seeds.map(s => Gen.const(999L).pureApply(params, s))

  @Benchmark
  def int_(): List[Int] =
    seeds.map(s => Gen.choose(Int.MinValue, Int.MaxValue).pureApply(params, s))

  @Benchmark
  def double_(): List[Double] =
    seeds.map(s => Gen.choose(Double.MinValue, Double.MaxValue).pureApply(params, s))

  @Benchmark
  def optionInt(): List[Option[Int]] =
    seeds.map(s => Gen.option(genInt).pureApply(params, s))

  @Benchmark
  def eitherIntInt(): List[Either[Int, Int]] =
    seeds.map(s => Gen.either(genInt, genInt).pureApply(params, s))

  @Benchmark
  def listOfInt(): List[List[Int]] =
    seeds.map(s => Gen.listOf(genInt).pureApply(params, s))

  @Benchmark
  def identifier(): List[String] =
    seeds.map(s => Gen.identifier.pureApply(params, s))

  @Benchmark
  def asciiPrintableStr(): List[String] =
    seeds.map(s => Gen.asciiPrintableStr.pureApply(params, s))

  @Benchmark
  def arbitraryString(): List[String] =
    seeds.map(s => arbitrary[String].pureApply(params, s))

  private val gens = Vector.fill(10)(genInt)

  @Benchmark
  def sequence(): List[Seq[Int]] =
    seeds.map(s => Gen.sequence[Vector[Int], Int](gens).pureApply(params, s))

  private val items = (1 to 100).toVector

  @Benchmark
  def oneOf(): List[Int] =
    seeds.map(s => Gen.oneOf(items).pureApply(params, s))

  @Benchmark
  def zipIntInt(): List[(Int, Int)] =
    seeds.map(s => Gen.zip(genInt, genInt).pureApply(params, s))

  @Benchmark
  def mapOfIntInt(): List[Map[Int, Int]] =
    seeds.map(s => Gen.mapOf(Gen.zip(genInt, genInt)).pureApply(params, s))

  private val gf = {
    val g = Gen.listOf(genInt)
    Gen.frequency(1 -> g, 2 -> g, 3 -> g, 4 -> g, 5 -> g)
  }

  @Benchmark
  def staticFrequency(): List[List[Int]] =
    seeds.map(s => gf.pureApply(params, s))

  @Benchmark
  def dynamicFrequency(): List[List[Int]] =
    seeds.map { s =>
      val g = Gen.listOf(genInt)
      val gf = Gen.frequency(1 -> g, 2 -> g, 3 -> g, 4 -> g, 5 -> g)
      gf.pureApply(params, s)
    }

  private def fg = Gen.choose(1, 100).filter(_ >= 3)

  @Benchmark
  def testFilter(): List[List[Int]] =
    seeds.map { s =>
      Gen.listOfN(100, fg).pureApply(params, s)
    }
}
