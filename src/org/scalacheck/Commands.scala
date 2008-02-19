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
trait Commands {

  /** The abstract state data type. This type must be immutable. */
  type S

  private val bindings = scala.collection.mutable.Map.empty[S,Any]

  protected class Binding(private val key: S) {
    def get: Any = bindings.get(key) match {
      case None => error("No value bound")
      case Some(x) => x
    }
  }

  /** An abstract command */
  trait Command {
    protected[Commands] def run(s: S) = apply(s)
    protected def apply(s: S): Any
    def nextState(s: S): S
    def preCondition(s: S): Boolean = true
    def postCondition(s: S, r: Any): Boolean = true
  }

  /** A command that binds its result for later use */
  abstract class SetCommand extends Command {
    protected[Commands] override def run(s: S) = {
      val r = apply(s)
      bindings += ((s,r))
      r
    }

    final override def nextState(s: S): S = nextState(s, new Binding(s))
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

  private def runCommands(cmds: Cmds): Boolean = cmds match {
    case Cmds(Nil, _) => true
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
