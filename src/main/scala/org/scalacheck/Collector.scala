package org.scalacheck

import org.scalacheck.Test.{Result, TestCallback}
import org.scalacheck.util.FreqMap

/**
 * Created with IntelliJ IDEA.
 * Author: Edmondo Porcu
 * Date: 28/02/14
 * Time: 12:38
 *
 */
trait NumberCollector extends TestCallback {

  def failOnNotANumber: Boolean

  def freqMapBackToSamples(map: FreqMap[Set[Any]]): Traversable[Double] = {
    map.getCounts.map{
      case (values, count) => repeat(values,count)
    }.flatten
  }

  def repeat(set: Set[Any], count: Int):Traversable[Double] = {
    val typedSet = set.flatMap {
      anyVal => try {
        Some(anyVal.toString.toDouble)
      }
      catch {
        case nf: NumberFormatException => if (failOnNotANumber) throw nf else None
      }
    }
    Vector.fill(count)(typedSet).flatten
  }

}

trait StatisticsCollector extends NumberCollector {
  /** Called whenever a property has finished testing */

  case class Statistics (count:Int, avg:Double, stdDev:Double, max:Double, min:Double){
    override def toString =s"Samples: $count average: $avg standard deviation: $stdDev, max: $max, min: $min"
  }

  object Statistics {
      case class Collector private(total:Double, max:Double, min:Double, samplesCount:Int){
        def add(sample:Double) = copy(total=total+sample, max = math.max(max,sample), min=math.min(min,sample), samplesCount=samplesCount+1)
        lazy val avg = total / samplesCount
        def this() = this(0,Double.MinValue, Double.MaxValue,0)
      }


    def apply(items:Traversable[Double]):Statistics = {
      val start = new Collector()
      val end = items.foldLeft(start){
        case(state,sample) => state add sample
      }

      val stdDev = items.map { x => math.sqrt( (x-end.avg) * (x-end.avg) ) }.sum / end.samplesCount
      Statistics(end.samplesCount,end.avg,stdDev,end.max,end.min)

    }

  }

  override def onTestResult(name: String, result: Result): Unit = {
    println(s"Statistics of test $name")
    val statistics = Statistics apply freqMapBackToSamples(result.freqMap)
    println(statistics)
  }


}

