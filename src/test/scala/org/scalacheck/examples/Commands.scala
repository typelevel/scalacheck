/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2011 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import Gen._
import Prop.{forAll, someFailing, noneFailing}
import Arbitrary._
import Shrink._
import java.util.concurrent.atomic.AtomicInteger

object CommandsExamples extends Properties("CommandsExamples") {

  property("ex1") = Example1

  private object Example1 extends Commands {

    case class Counter(private var n: AtomicInteger) {
      def this(int: Int) = this(new AtomicInteger(int))
      def inc = n.incrementAndGet
      def dec = n.decrementAndGet
      def get = n.get
    }

    case class State (n: Int, mutable: Counter) {
      def this(n: Int, mutable: Int) = this(n, new Counter(mutable))
    }

    def initialState = new State(0, 0)

    def genCommand(s: State): Gen[Command] = oneOf(Inc, Dec, Get)

    case object Inc extends Command {
      def nextState(s: State) = s.copy(n = s.n + 1)
      def run(s: State) = s.mutable.inc
      postConditions += {
        case (s0,s1,r) => r == s1.n
      }
    }
     
    case object Dec extends Command {
      def nextState(s: State) = s.copy(n = s.n - 1)
      def run(s: State) = s.mutable.dec
      postConditions += {
        case (s0,s1,r) => r == s1.n
      }
    }
     
    case object Get extends Command {
      def nextState(s: State) = s
      def run(s: State) = s.mutable.get
      postConditions += {
        case (s0,s1,r) => r == s1.n
      }
    }
  }
}
