import org.scalacheck.Gen
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.commands.Commands

import org.iq80.leveldb._
import org.fusesource.leveldbjni.JniDBFactory._



object CommandsLevelDB extends org.scalacheck.Properties("CommandsLevelDB") {

  property("leveldbspec") = LevelDBSpec.property

}

object LevelDBSpec extends Commands {

  case class State(
    open: Boolean,
    name: String,
    contents: Map[String,String]
  )

  case class Sut(
    var name: String,
    var db: Option[DB]
  ) {
    def path = s"db_$name"
  }

  def canCreateNewSut(newState: State, initSuts: Traversable[State],
    runningSuts: Traversable[Sut]
  ) = {
    !initSuts.exists(_.name == newState.name) &&
    !runningSuts.exists(_.name == newState.name)
  }

  def newSutInstance(state: State): Sut = Sut(state.name, None)

  def destroySutInstance(sut: Sut) = sut.db.foreach(_.close)

  def initialPreCondition(state: State) = !state.open

  val genInitialState = for {
    name <- Gen.listOfN(8, Gen.alphaLowerChar).map(_.mkString)
  } yield State(false, name, Map.empty)

  def genCommand(state: State): Gen[Command] =
    if(state.open) Gen.const(Close)
    else Gen.const(Open)

  case object Open extends Command {
    type Result = Unit
    def run(sut: Sut) = {
      val options = new Options()
      options.createIfMissing(true)
      sut.db = Some(factory.open(new java.io.File(sut.path), options))
    }
    def nextState(state: State) = state.copy(open = true)
    def preCondition(state: State) = !state.open
    def postCondition(state: State, result: Result) = true
  }

  case object Close extends Command {
    type Result = Unit
    def run(sut: Sut) = {
      sut.db.foreach(_.close)
      sut.db = None
    }
    def nextState(state: State) = state.copy(open = false)
    def preCondition(state: State) = state.open
    def postCondition(state: State, result: Result) = true
  }

}
