import org.scalacheck.Gen
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.commands.Commands

import org.iq80.leveldb._
import org.fusesource.leveldbjni.JniDBFactory._

import scala.util.{Try, Success}
import scala.collection.immutable.Map


object CommandsLevelDB extends org.scalacheck.Properties("CommandsLevelDB") {

  property("leveldbspec") = LevelDBSpec.property()

}

object LevelDBSpec extends Commands {

  case class State(
    open: Boolean,
    name: String,
    contents: Map[List[Byte],List[Byte]]
  )

  case class Sut(
    var name: String,
    var db: DB
  ) {
    def path = s"db_$name"
  }

  def canCreateNewSut(newState: State, initSuts: Iterable[State],
    runningSuts: Iterable[Sut]
  ) = {
    !initSuts.exists(_.name == newState.name) &&
    !runningSuts.exists(_.name == newState.name)
  }

  def newSut(state: State): Sut = Sut(state.name, null)

  def destroySut(sut: Sut) = if(sut.db != null) sut.db.close

  def initialPreCondition(state: State) = !state.open

  val genInitialState = for {
    name <- Gen.listOfN(8, Gen.alphaLowerChar).map(_.mkString)
  } yield State(false, name, Map.empty)

  def genCommand(state: State): Gen[Command] =
    if (!state.open) Gen.oneOf(Open, Close)
    else Gen.oneOf(
      Gen.const(Close),
      genPut,
      genPutExisting(state),
      genGet,
      genGetExisting(state)
    )

  val genPut: Gen[Put] = Gen.resultOf(Put)

  def genPutExisting(state: State): Gen[Put] = for {
    key <- Gen.oneOf(state.contents.keys.toSeq)
    value <- Gen.oneOf(arbitrary[List[Byte]],
                       Gen.const(state.contents(key)))
  } yield Put(key,value)

  val genGet: Gen[Get] = Gen.resultOf(Get)

  def genGetExisting(state: State): Gen[Get] = for {
    key <- Gen.oneOf(state.contents.keys.toSeq)
  } yield Get(key)

  case object Open extends UnitCommand {
    def run(sut: Sut) = sut.synchronized {
      val options = new Options()
      options.createIfMissing(true)
      sut.db = factory.open(new java.io.File(sut.path), options)
    }
    def nextState(state: State) = state.copy(open = true)
    def preCondition(state: State) = !state.open
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

  case class Put(key: List[Byte], value: List[Byte]) extends UnitCommand {
    def run(sut: Sut) = sut.synchronized {
      sut.db.put(key.toArray, value.toArray)
    }
    def preCondition(state: State) = state.open
    def nextState(state: State) = state.copy(
      contents = state.contents + (key -> value)
    )
    def postCondition(state: State, success: Boolean) = success
  }

  case class Get(key: List[Byte]) extends Command {
    type Result = List[Byte]
    def run(sut: Sut) = sut.synchronized {
      sut.db.get(key.toArray).toList
    }
    def preCondition(state: State) = state.open
    def nextState(state: State) = state
    def postCondition(state: State, result: Try[List[Byte]]) =
      state.contents.get(key) match {
        case None => result.isFailure
        case Some(value) => result == Success(value)
      }
  }

}
