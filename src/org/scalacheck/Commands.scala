/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2008 Rickard Nilsson. All rights reserved.          **
**  http://code.google.com/p/scalacheck/                                   **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*-------------------------------------------------------------------------*/

package org.scalacheck

import Gen._
import Prop._
import Shrink._

/** See User Guide for usage examples */
trait Commands extends Prop {

  /** The abstract state data type. This type must be immutable. */
  type S

  private val bindings = scala.collection.mutable.Map.empty[Int,Any]

  protected class Binding(private val key: Int) {
    def get: Any = bindings.get(key) match {
      case None => error("No value bound")
      case Some(x) => x
    }
  }

  def apply(p: Gen.Params) = commandsProp(p)

  /** An abstract command */
  trait Command {
    final override def hashCode = super.hashCode

    /** Used internally. */
    protected[Commands] def run_(s: S) = run(s)

    def run(s: S): Any
    def nextState(s: S): S
    var preCondition: S => Boolean = s => true
    var postCondition: (S,Any) => Prop = (s,r) => proved
  }

  /** A command that binds its result for later use */
  trait SetCommand extends Command {
    /** Used internally. */
    protected[Commands] final override def run_(s: S) = {
      val r = run(s)
      bindings += ((s.hashCode,r))
      r
    }

    final def nextState(s: S) = nextState(s, new Binding(s.hashCode))
    def nextState(s: S, b: Binding): S
  }

  /** Resets the system under test and returns its abstract state */
  protected def initialState(): S

  private def initState() = {
    bindings.clear()
    initialState()
  }

  /** Generates a command */
  protected def genCommand(s: S): Gen[Command]

  private case class Cmds(cs: List[Command], ss: List[S]) {
    override def toString = cs.map(_.toString).mkString(", ")
  }

  private def genCmds: Gen[Cmds] = {
    def sizedCmds(s: S)(sz: Int): Gen[Cmds] =
      if(sz <= 0) value(Cmds(Nil, Nil)) else for {
        c <- genCommand(s) suchThat (_.preCondition(s))
        Cmds(cs,ss) <- sizedCmds(c.nextState(s))(sz-1)
      } yield Cmds(c::cs, s::ss)

    for {
      s0 <- value(() => initialState())
      cmds <- sized(sizedCmds(s0))
    } yield cmds
  }

  private def validCmds(s: S, cs: List[Command]): Option[Cmds] =
    cs match {
      case Nil => Some(Cmds(Nil, s::Nil))
      case c::_ if !c.preCondition(s) => None
      case c::cmds => for {
        Cmds(_, ss) <- validCmds(c.nextState(s), cmds)
      } yield Cmds(cs, s::ss)
    }

  private def runCommands(cmds: Cmds): Prop = cmds match {
    case Cmds(Nil, _) => proved
    case Cmds(c::cs, s::ss) =>
      c.postCondition(s,c.run(s)) && runCommands(Cmds(cs,ss))
    case _ => error("Should not be here")
  }

  def commandsProp: Prop = {

    def shrinkCmds(cmds: Cmds) = cmds match { case Cmds(cs,_) =>
      shrink(cs).flatMap(cs => validCmds(initialState(), cs).toList)
    }

    forAllShrink(genCmds label "COMMANDS", shrinkCmds)(runCommands _)

  }

}
