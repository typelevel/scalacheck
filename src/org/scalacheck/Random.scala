package org.scalacheck

trait RandomGenerator {
  def choose(low: Int, high: Int): Int
  def choose(low: Double, high: Double): Double
}

object StdRand extends RandomGenerator {

  private val r = new java.util.Random

  def choose(l: Int, h: Int) =
    if (h <= l) h
    else l + r.nextInt((h-l)+1)

  def choose(l: Double, h: Double) =
    if (h <= l) h
    else Math.random * (h-l) + l

}
