package scalacheck

import Gen._
import Prop._
import Arbitrary._

trait Commands {

  /** The abstract state data type */
  type S

  /** An abstract command */
  trait Command {
    def apply(s: S): Any
    def nextState(s: S): S
    def preCondition(s: S): Boolean = true
    def postCondition(s: S, r: Any): Boolean = true
  }

  /** Resets the system under test and returns its abstract state */
  protected def initialState(): S

  /** Generates a command */
  protected def genCommand(s: S): Gen[Command]

  /** Generates a sequence of commands */
  private def genCommands(s: S) = {

    def sizedCmds(s: S)(sz: Int): Gen[List[Command]] =
      if(sz <= 0) value(Nil) else for {
        c  <- genCommand(s) suchThat (_.preCondition(s))
        cs <- sizedCmds(c.nextState(s))(sz-1)
      } yield c::cs

    sized(sizedCmds(s))

  }

  /** Verifies that a command sequence is valid */
  private def validCommands(s: S, cmds: List[Command]): Boolean = cmds match {
    case Nil   => true
    case c::cs => c.preCondition(s) && validCommands(c.nextState(s),cs)
  }

  /** Executes a command sequence and returns true iff all postconditions
   *  are fulfilled */
  def runCmds(s: S, cmds: List[Command]): Boolean = cmds match {
    case Nil   => true
    case c::cs => c.postCondition(s,c(s)) && runCmds(c.nextState(s),cs)
  }

  /** A property that holds iff all valid command sequences fulfills
   *  the postconditions. */
  def commandsProp: Prop = {

    case class Cmds(cmds: List[Command], sf: () => S) {
      override def toString = cmds.map(_.toString).mkString(", ")
    }

    /* TODO Hack to be able to shrink command sequences. Maybe we should
     * split the Arbitrary type into Arbitrary and Shrink? */
    implicit def arbCommand(x: Arb[Command]) = new Arbitrary[Command] {
      def getArbitrary = value(null)
    }

    def genCmds = for {
      s <- value(() => initialState())
      cmds <- genCommands(s)
    } yield Cmds(cmds, () => s)

    def shrinkCmds(cmds: Cmds) = cmds match {
      case Cmds(cs,_) => shrink(cs).map(cs => Cmds(cs, () => initialState()))
    }

    forAllShrink(genCmds label "COMMANDS", shrinkCmds) { case Cmds(cs, sf) =>
      val s = sf()
      validCommands(s,cs) ==> runCmds(s,cs).addArg(Arg("INISTATE",s,0))
    }
  }

}
