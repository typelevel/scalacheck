/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2011 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*-------------------------------------------------------------------------*/

package org.scalacheck

import Gen._
import Prop.{forAll, someFailing, noneFailing}
import Arbitrary._
import Shrink._

object CommandsSpecification extends Properties("Commands") {

  property("setcommands") = SimpleSetCommandsSpec

  private object SimpleSetCommandsSpec extends Commands {
    case class State (
      results: List[Binding]
    )

    def initialState = State(Nil)

    def genCommand(s: State): Gen[Command] = for {
      n <- arbitrary[Int]
    } yield SimpleSetCommand(n)

    case class SimpleSetCommand(n: Int) extends SetCommand {
      def nextState(s: State, b: Binding) = s.copy(results = s.results :+ b)
      def run(s: State) = n
      postConditions += {
        case (s0,s1,r) =>
          (s1.results.length == s0.results.length+1) &&
          s1.results.last.get == n
      }
    }
  }
}
