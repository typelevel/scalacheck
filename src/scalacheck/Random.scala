package scalacheck

trait RandomGenerator {
  def choose(inclusiveRange: (Int,Int)): Int
}

object StdRand extends RandomGenerator {
  import scala.Math._
  private val r = new java.util.Random
  def choose(range: (Int,Int)) = range match {
    case (l,h) if(l == h) => l
    case (l,h) if(h < l)  => h
    case (l,h)            => l + r.nextInt((h-l)+1)
  }
}
