package org.scalacheck.rng

import java.lang.Integer.rotateLeft
import scala.util.Random.nextInt

/**
 * Simple RNG by Bob Jenkins:
 *
 * http://burtleburtle.net/bob/rand/smallprng.html
 */
class Seed(a: Int, b: Int, c: Int, private[rng] val d: Int) {

  /**
   * Generate the next seed in the RNG's sequence.
   */
  def next: Seed = {
    val e = a - rotateLeft(b, 23)
    val a1 = b ^ rotateLeft(c, 16)
    val b1 = c + rotateLeft(d, 11)
    val c1 = d + e
    val d1 = e + a
    new Seed(a1, b1, c1, d1)
  }

  /**
   * Reseed the RNG using the given Long value.
   */
  def reseed(n: Long): Seed = {
    val n0 = ((n >>> 32) & 0xffffffff).toInt
    val n1 = (n & 0xffffffff).toInt
    var i = 0
    var seed = new Seed(a ^ n0, b ^ n1, c, d)
    while(i < 16) { seed = seed.next; i += 1 }
    seed
  }
}

object Seed {

  /**
   * Generate a random seed.
   *
   * No cycles will be expected in less than 2^64 results.
   */
  def random(): Seed = {
    val s = nextInt
    var i = 0
    var seed = new Seed(0xf1ea5eed, s, s, s)
    while (i < 20) { seed = seed.next; i += 1 }
    seed
  }
}
