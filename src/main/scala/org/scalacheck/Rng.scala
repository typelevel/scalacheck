package org.scalacheck

object Rng {

  def randomSeed(): Long =
    scala.util.Random.nextLong

  def next(seed: Long): Long =
    6364136223846793005L * seed + 1442695040888963407L

  def boolean(seed: Long): Boolean =
    seed >= 0L

  def int(seed: Long): Long =
    ((seed >>> 32) & 0xffffffffL).toInt

  def long(seed: Long): Long =
    seed

  def float(seed: Long): Float =
    (int(seed) >>> 8) * 5.9604645e-8f

  def double(seed: Long): Double =
    (seed >>> 11) * 1.1102230246251565e-16

  def values(seed: Long): Stream[Long] =
    seed #:: values(next(seed))
}
