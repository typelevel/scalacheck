package org.scalacheck.commands

import org.scalacheck._
import org.scalacheck.commands._
import org.scalacheck.Gen._
import scala.util.{Success, Failure, Try}
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets
import java.io.PrintWriter
import java.io.File
import org.scalacheck.Properties

class PidSpawner() {
  case class PidSpawnerState(
    pids: Set[String],
    regs: Map[String, String])

  var state = PidSpawnerState(pids = Set.empty, regs = Map.empty)

  def genUUID() = java.util.UUID.randomUUID.toString

  def spawn():String = {
    val uuid = genUUID()
    state = state.copy(pids = state.pids + uuid)
    uuid
  }

  def register(uuid: String, name: String): Unit = {
    if(state.pids.exists(x => x == uuid) && !state.regs.exists(x => x._1 == name)) {
      state = state.copy(regs = state.regs ++ Map(name -> uuid))
    } else {
      throw new Exception(s"Unable to register ${name} -> ${uuid}.")
    }
  }

  def unregister(name: String): Unit = {
    if(state.regs.contains(name)) {
      state = state.copy(regs = state.regs - name)
    } else {
      throw new Exception(s"Unable to unregister ${name}")
    }
  }

  def whereis(name: String): Option[String] = {
    state.regs.find(_._1 == name).map { case (n,u) => u }
  }
}

object CommandsPidRegistration extends Properties("CommandsPidRegistration") {
  import org.scalacheck.Test._
  
  property("pidregspec") = PidRegistrationSpecification.property()
}

object PidRegistrationSpecification extends Commands{
  
  type Sut = PidSpawner

  case class State(
    pids: Set[Term[String]],
    regs: Map[String, Term[String]])

  override def genInitialState: Gen[State] = State(
    pids = Set.empty,
    regs = Map.empty)


  override def canCreateNewSut(newState: State, initSuts: Traversable[State],
                               runningSuts: Traversable[Sut]
                                ): Boolean = {
    initSuts.isEmpty && runningSuts.isEmpty
  }

  override def destroySut(sut: Sut): Unit = {
  }

  override def initialPreCondition(state: State): Boolean = true

  override def newSut(state: State): Sut = {
    new PidSpawner()
  }
  
  def genCommand(state: State): Gen[Command] = {
    frequency(
      (50, genSpawn),
      (20, genRegister(state)),
      (20, genUnregister(state)),
      (5, genWhereIsRandom),
      (20, genWhereIs(state)),
      (5, genUnregisterRandom),
      (20, genUnregister(state))
    )
  }

  def genSpawn: Gen[Spawn] = Spawn()

  def genRegister(state: State): Gen[Command] = {
    if(state.pids.isEmpty) genSpawn else for {
      SymbVar(binding) <- Gen.oneOf(state.pids.toSeq)
      name <- Gen.identifier
    } yield Register(binding, name)
  }

  def genUnregisterRandom: Gen[Command] = {
    for {
      id <- identifier
    } yield Unregister(id)
  }

  def genUnregister(state: State): Gen[Command] = {
    if(state.regs.isEmpty) genUnregisterRandom else for {
      (name,_) <- oneOf(state.regs.toSeq)
    } yield Unregister(name)
  }

  def genWhereIsRandom: Gen[Command] = {
    for {
      id <- Gen.identifier
    } yield WhereIs(id)
  }

  def genWhereIs(state: State): Gen[Command] = {
    if(state.regs.isEmpty) genWhereIsRandom else for {
      (id,_) <- oneOf(state.regs.toSeq)
    } yield WhereIs(id)
  }

  case class Spawn() extends Command {
    
    override type Result = String

    override def preCondition(s: State): Boolean = true

    override def nextState(s: State, v:Term[Result]) = {
      s.copy(pids = s.pids + v)
    }

    override def postCondition(s: State, result: Try[Result]): Prop = {
      result.isSuccess
    }

    override def run(sut: Sut, s: State): Result = {
      sut.spawn()
    }
  }

  case class Register(bind: Binding, name: String) extends Command {
    def findPid(s: State) = s.pids.find(_.binding == bind)
    override type Result = Unit
    override def preCondition(state: State): Boolean = findPid(state).isDefined
    override def nextState(s: State, v:Term[Result]) = {
      if(s.regs.contains(name)) {
        s
      } else {
        findPid(s) map { case t => s.copy(regs = s.regs ++ Map(name -> t)) } getOrElse(s)
      }
    }

    override def postCondition(s: State, result: Try[Result]): Prop = {
      if(!s.regs.contains(name)) result.isSuccess
      else result.isFailure
    }
    
    override def run(sut: Sut, s: State): Result = {
      val pid = findPid(s).valueOrElse("Invalid PID")
      sut.register(pid, name)
    }
  }

  case class WhereIs(name: String) extends Command {
    override type Result = Option[String]

    override def preCondition(s: State): Boolean = true

    override def nextState(s: State, v:Term[Result]) = s

    override def postCondition(s: State, result: Try[Result]): Prop = {
      result match {
        case Success(opt) => opt == s.regs.find(_._1 == name).map(_._2).valueOpt
        case Failure(e) => Prop.exception(e)
      }
    }

    override def run(sut: Sut, s: State): Result = {
      sut.whereis(name)
    }
  }
  
  case class Unregister(name: String) extends Command {
    override type Result = Unit

    def prep(s: State) = s.regs.contains(name)
    
    override def preCondition(state: State): Boolean = true

    override def nextState(s: State, v:Term[Result]) = {
      if(prep(s)) s.copy(regs = s.regs - name)
      else s
    }

    override def postCondition(s: State, result: Try[Result]): Prop = {
      if(result.isSuccess) prep(s)
      else !prep(s)
    }

    override def run(sut: Sut, s: State): Result = {
      sut.unregister(name)
    }
  }
}