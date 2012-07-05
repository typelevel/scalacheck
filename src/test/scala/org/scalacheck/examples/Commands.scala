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

object CommandsExamples extends Properties("CommandsExamples") {

  property("ex1") = Example1

  private object Example1 extends Commands {

    case class Counter(private var n: Int) {
      def inc = {
        n += 1
        n
      }

      def dec = {
        n -= 1
        n
      }

      def get = n
    }

    var sut = Counter(0)

    case class State (
      n: Int
    )

    def initialState = {
      sut = Counter(0)
      State(0)
    }

    def genCommand(s: State): Gen[Command] = oneOf(Inc, Dec, Get)

    case object Inc extends Command {
      def nextState(s: State) = s.copy(s.n + 1)
      def run(s: State) = sut.inc
      postConditions += {
        case (s0,s1,r) => r == s1.n
      }
    }

    case object Dec extends Command {
      def nextState(s: State) = s.copy(s.n - 1)
      def run(s: State) = sut.dec
      postConditions += {
        case (s0,s1,r) => r == s1.n
      }
    }

    case object Get extends Command {
      def nextState(s: State) = s
      def run(s: State) = sut.get
      postConditions += {
        case (s0,s1,r) => r == s1.n
      }
    }

  }

}
