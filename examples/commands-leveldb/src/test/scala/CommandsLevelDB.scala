import org.scalacheck.Gen
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.commands.Commands

import org.iq80.leveldb._
import org.fusesource.leveldbjni.JniDBFactory._

import scala.util.Try


object CommandsLevelDB extends org.scalacheck.Properties("CommandsLevelDB") {

  property("leveldbspec") = LevelDBSpec.property

}

object LevelDBSpec extends Commands {

  override val threadCount = 2;

  case class State(
    open: Boolean,
    name: String,
    contents: Map[String,String]
  )

  case class Sut(
    var name: String,
    var db: DB
  ) {
    def path = s"db_$name"
  }

  def canCreateNewSut(newState: State, initSuts: Traversable[State],
    runningSuts: Traversable[Sut]
  ) = {
    !initSuts.exists(_.name == newState.name) &&
    !runningSuts.exists(_.name == newState.name)
  }

  def newSutInstance(state: State): Sut = Sut(state.name, null)

  def destroySutInstance(sut: Sut) = if(sut.db != null) sut.db.close

  def initialPreCondition(state: State) = !state.open

  val genInitialState = for {
    name <- Gen.listOfN(8, Gen.alphaLowerChar).map(_.mkString)
  } yield State(false, name, Map.empty)

  def genCommand(state: State): Gen[Command] = Gen.oneOf(Open,Close)

  case object Open extends UnitCommand {
    def run(sut: Sut) = sut.synchronized {
      val options = new Options()
      options.createIfMissing(true)
      sut.db = factory.open(new java.io.File(sut.path), options)
    }
    def nextState(state: State) = state.copy(open = true)
    def preCondition(state: State) = true
    def postCondition(state: State, success: Boolean) =
      state.open != success
  }

  case object Close extends UnitCommand {
    def run(sut: Sut) = sut.synchronized {
      sut.db.close
      sut.db = null
    }
    def nextState(state: State) = state.copy(open = false)
    def preCondition(state: State) = true
    def postCondition(state: State, success: Boolean) =
      state.open == success
  }

}
