package bench

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

import org.scalacheck.Gen
import org.scalacheck.rng.Seed

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class GenBench {

  // if we only benchmark with one seed, we might get lucky (or
  // unlucky) for a particular generator. by benchmarking with 10
  // different seeds (constant between runs) we reduce the chance of
  // extreme luckiness or unluckness. that said, a generator that hits
  // a 100x slow down 1% of the time will likely cause problems for
  // this benchmarking strategy.
  val preseeds: List[Long] =
    List(
      -4522249856328521867L, 624585044422885589L, 3202652801110979260L,
      -8551572270962952301L, 2834982173645900841L, -7161929398350763557L,
      -249952150141786145L, -7514897450101711682L, 9111269543303992388L,
      -8189142734131299482L)

  val seeds: List[Seed] =
    preseeds.map(n => Seed(n))

  val params: Gen.Parameters =
    Gen.Parameters.default

  val genInt: Gen[Int] =
    Gen.choose(Int.MinValue, Int.MaxValue)

  @Benchmark
  def const_(): List[Option[Long]] =
    seeds.map(s => Gen.const(999L)(params, s))

  @Benchmark
  def int_(): List[Option[Int]] =
    seeds.map(s => Gen.choose(Int.MinValue, Int.MaxValue).apply(params, s))

  @Benchmark
  def double_(): List[Option[Double]] =
    seeds.map(s => Gen.choose(Double.MinValue, Double.MaxValue).apply(params, s))

  @Benchmark
  def optionInt(): List[Option[Option[Int]]] =
    seeds.map(s => Gen.option(genInt)(params, s))

  @Benchmark
  def eitherIntInt(): List[Option[Either[Int, Int]]] =
    seeds.map(s => Gen.either(genInt, genInt)(params, s))

  @Benchmark
  def listOfInt(): List[Option[List[Int]]] =
    seeds.map(s => Gen.listOf(genInt)(params, s))

  @Benchmark
  def identifier(): List[Option[String]] =
    seeds.map(s => Gen.identifier(params, s))

  @Benchmark
  def asciiPrintableStr(): List[Option[String]] =
    seeds.map(s => Gen.asciiPrintableStr(params, s))

  // @Benchmark
  // def unicodeStr(): List[Option[String]] =
  //   seeds.map(s => Gen.unicodeStr(params, s))

  private val gens = Vector.fill(10)(genInt)

  @Benchmark
  def sequence(): List[Option[Seq[Int]]] =
    seeds.map(s => Gen.sequence[Vector[Int], Int](gens).apply(params, s))

  private val items = (1 to 100).toVector

  @Benchmark
  def oneOf(): List[Option[Int]] =
    seeds.map(s => Gen.oneOf(items)(params, s))

  @Benchmark
  def zipIntInt(): List[Option[(Int, Int)]] =
    seeds.map(s => Gen.zip(genInt, genInt)(params, s))

  @Benchmark
  def mapOfIntInt(): List[Option[Map[Int, Int]]] =
    seeds.map(s => Gen.mapOf(Gen.zip(genInt, genInt))(params, s))

  private val gf = {
    val g = Gen.listOf(genInt)
    Gen.frequency(1 -> g, 2 -> g, 3 -> g, 4 -> g, 5 -> g)
  }

  @Benchmark
  def staticFrequency(): List[Option[List[Int]]] =
    seeds.map(s => gf(params, s))

  @Benchmark
  def dynamicFrequency(): List[Option[List[Int]]] =
    seeds.map { s =>
      val g = Gen.listOf(genInt)
      val gf = Gen.frequency(1 -> g, 2 -> g, 3 -> g, 4 -> g, 5 -> g)
      gf(params, s)
    }
}
