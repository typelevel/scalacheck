/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2016 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck
package util

import language.higherKinds

import collection._

object BuildableSpecification {
  def container[C[_]](implicit
    evb: Buildable[String, C[String]],
    evt: C[String] => Traversable[String]
  ) = Gen.containerOf[C, String](Gen.alphaStr)

  implicit val listGen: Gen[List[String]] = container[List]

  implicit val streamGen: Gen[Stream[String]] = container[Stream]

  implicit val arrayGen: Gen[Array[String]] = container[Array]

  implicit val mutableSetGen: Gen[mutable.Set[String]] = container[mutable.Set]

  implicit val setGen: Gen[Set[String]] = container[Set]
    
  implicit val immutableSortedSetGen: Gen[immutable.SortedSet[String]] = container[immutable.SortedSet]

  implicit val vectorGen: Gen[Vector[String]] = container[Vector]

  implicit val treeSetGen: Gen[immutable.TreeSet[String]] = container[immutable.TreeSet]

  implicit val hashSetGen: Gen[immutable.HashSet[String]] = container[immutable.HashSet]

  implicit val indexedSeqGen: Gen[immutable.IndexedSeq[String]] = container[immutable.IndexedSeq]

  implicit val seqGen: Gen[immutable.Seq[String]] = container[immutable.Seq]

  implicit val iterableGen: Gen[immutable.Iterable[String]] = container[immutable.Iterable]

  implicit val trieIteratorGen: Gen[immutable.Queue[String]] = container[immutable.Queue]
}
