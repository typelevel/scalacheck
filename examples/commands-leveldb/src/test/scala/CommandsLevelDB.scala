import org.scalacheck.Gen
import org.scalacheck.commands.Commands
import org.iq80.leveldb._
import org.fusesource.leveldbjni.JniDBFactory._

object CommandsLevelDB extends org.scalacheck.Properties("CommandsLevelDB") {

  property("leveldbspec") = LevelDBSpec.property

}

object LevelDBSpec extends Commands {

  case class State(
    dbname: String,
    dbcontents: Map[String,String]
  )

  type Sut = DB

  def canCreateNewSut(newState: State, initStates: Traversable[State]) = true

  def newSutInstance(state: State): Sut = {
    val options = new Options()
    options.createIfMissing(true)
    factory.open(new java.io.File(state.dbname), options);
  }

  def destroySutInstance(db: Sut) = db.close

  def initialPreCondition(state: State) = true

  val genInitialState = for {
    name <- Gen.listOfN(8, Gen.alphaLowerChar).map(_.mkString)
  } yield State(s"db_$name", Map.empty)

  def genCommand(state: State): Gen[Command] = Gen.const(NoOp)

}
