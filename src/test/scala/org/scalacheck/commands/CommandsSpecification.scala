/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2014 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck.commands

import org.scalacheck._

object CommandsSpecification extends Properties("Commands") {

  property("commands") = TestCommands.property

  object TestCommands extends Commands {
    case class Counter(var n: Int) {
      val lock = new concurrent.Lock
      //def inc = { lock.acquire; n = n + 1; val r = n; lock.release; r }
      //def dec = { lock.acquire; n = n - 1; val r = n; lock.release; r }
      //def reset = { lock.acquire; n = 0; val r = n; lock.release; r }
      def inc = { val r = n + 1; n = r; r }
      def dec = { val r = n - 1; n = r; r }
      def reset = { n = 0; val r = n; r }
    }

    type Sut = Counter
    type State = Int

    def canCreateNewSut(newState: State, initSuts: Traversable[State],
      runningSuts: Traversable[Sut]) = true

    def newSutInstance(state: State): Sut = Counter(state)

    def destroySutInstance(sut: Sut) = {}

    def initialPreCondition(state: State) = true

    val genInitialState = Gen.choose(0,100)

    def genCommand(state: State): Gen[Command] = Gen.oneOf(Inc, Dec, Reset)

    case object Inc extends SuccessCommand {
      type Result = Int
      def run(sut: Sut) = sut.inc
      def nextState(state: State) = state+1
      def preCondition(state: State) = true
      def postCondition(state: State, result: Result) = result == state+1
    }

    case object Dec extends SuccessCommand {
      type Result = Int
      def run(sut: Sut) = sut.dec
      def nextState(state: State) = state-1
      def preCondition(state: State) = true
      def postCondition(state: State, result: Result) = result == state-1
    }

    case object Reset extends SuccessCommand {
      type Result = Int
      def run(sut: Sut) = sut.reset
      def nextState(state: State) = 0
      def preCondition(state: State) = true
      def postCondition(state: State, result: Result) = result == 0
    }
  }

}
