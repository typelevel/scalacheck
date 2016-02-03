/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2016 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck.commands

import org.scalacheck._

object CommandsSpecification extends Properties("Commands") {

  property("commands") = TestCommands.property(threadCount = 4)

  object TestCommands extends Commands {
    case class Counter(var n: Int) {
      val lock = new java.util.concurrent.locks.ReentrantLock
      def inc = { lock.lock; n += 1; lock.unlock; n }
      def dec = { lock.lock; n -= 1; lock.unlock; n }
      //def inc = { n += 1; n }
      //def dec = { n -= 1; n }
      def get = n
    }

    type Sut = Counter
    type State = Int

    override def shrinkState: Shrink[Int] = implicitly

    def canCreateNewSut(newState: State, initSuts: Traversable[State],
      runningSuts: Traversable[Sut]) = true

    def newSut(state: State): Sut = Counter(state)

    def destroySut(sut: Sut) = {}

    def initialPreCondition(state: State) = true

    val genInitialState = Gen.choose(0,100)

    def genCommand(state: State): Gen[Command] =
      Gen.oneOf(Get, Inc, Dec)

    case object Get extends SuccessCommand {
      type Result = Int
      def run(sut: Sut) = sut.get
      def nextState(state: State) = state
      def preCondition(state: State) = true
      def postCondition(state: State, result: Result) = result == state
    }

    case object Inc extends SuccessCommand {
      type Result = Int
      def run(sut: Sut) = sut.inc
      def nextState(state: State) = state+1
      def preCondition(state: State) = true
      def postCondition(state: State, result: Result) = result == (state+1)
    }

    case object Dec extends SuccessCommand {
      type Result = Int
      def run(sut: Sut) = sut.dec
      def nextState(state: State) = state-1
      def preCondition(state: State) = true
      def postCondition(state: State, result: Result) = result == (state-1)
    }
  }

}
