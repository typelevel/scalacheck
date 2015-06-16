package org.scalacheck.rng

/**
 * Use a seed to generate random values.
 *
 * Since this is an immutable RNG, rng.Seed is where the action
 * is. This object just abstracts over producing Long/Double values
 * along with returning the next seed.
 */
object Rng {

  def randomSeed(): Seed = Seed.random()

  def next(seed: Seed): Seed = seed.next

  /**
   * Generates a Long value.
   *
   * The values will be uniformly distributed.
   */
  def long(seed: Seed): (Long, Seed) = {
    val n0 = seed.d.toLong
    val s0 = seed.next
    val n1 = s0.d.toLong
    val s1 = s0.next
    ((n0 << 32) | n1, s1)
  }

  /**
   * Generates a Double value.
   *
   * The values will be uniformly distributed, and will be contained
   * in the interval [0.0, 1.0).
   */
  def double(seed: Seed): (Double, Seed) = {
    val n0 = seed.d.toLong
    val s0 = seed.next
    val n1 = s0.d.toLong
    val s1 = s0.next
    val n = (n0 << 32) | n1
    ((n >>> 11) * 1.1102230246251565e-16, s1)
  }
}
