package org.scalacheck

import java.util.HashMap
import scala.collection.JavaConverters._
import scala.collection.immutable.IntMap
import org.scalacheck.Prop.{forAll, AnyOperators}

object IntMapSpec extends org.scalacheck.Properties("IntMap") {
  /** Compare a HashMap and an IntMap for equality */
  private def eqMaps(hm: HashMap[Int,Any], im: IntMap[Any]) = {
    im.keys.forall(hm.containsKey) &&
    hm.keySet.containsAll(im.keys.asJavaCollection) &&
    im.keys.forall(k => im(k) == hm.asScala(k))
  }

  /** Create an IntMap and a HashMap with the same contents */
  private def createMaps(l: List[Int]) = {
    val mappings = for(n <- l) yield (n, new Object)
    val im = IntMap(mappings: _*)
    val hm = new HashMap[Int, Any]
    for((n,x) <- mappings) hm.put(n,x)
    (hm, im)
  }

  property("size") = forAll { (l: List[Int]) =>
    val (refMap, intMap) = createMaps(l)
    intMap.size ?= refMap.size
  }
  property("isEmpty") = forAll { (l: List[Int]) =>
    val (refMap, intMap) = createMaps(l)
    intMap.isEmpty ?= refMap.isEmpty
  }
  property("add") = forAll { (l: List[Int], k: Int, v: String) =>
    val (refMap, intMap) = createMaps(l)
    refMap.put(k, v)
    eqMaps(refMap, intMap + (k -> v))
  }
  property("remove") = forAll { (l: List[Int], k: Int) =>
    val (refMap, intMap) = createMaps(l)
    refMap.remove(k)
    eqMaps(refMap, intMap - k)
  }
}
