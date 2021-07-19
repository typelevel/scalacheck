import org.scalacheck.Gen
import org.scalacheck.Gen.{someOf, oneOf, const, nonEmptyListOf,
  identifier, frequency}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.commands.Commands

import scala.util.{Try, Success}
import scala.collection.immutable.Map
import scala.collection.Seq

import com.redis.RedisClient


object CommandsRedis extends org.scalacheck.Properties("CommandsRedis") {

  property("redisspec") = RedisSpec.property()

}

object RedisSpec extends Commands {

  type Sut = RedisClient

  case class State (
    contents: collection.immutable.Map[String,String],
    deleted: collection.immutable.Set[String],
    connected: Boolean
  )

  def canCreateNewSut(newState: State, initSuts: Traversable[State],
    runningSuts: Traversable[Sut]
  ): Boolean = {
    initSuts.isEmpty && runningSuts.isEmpty
  }

  def destroySut(sut: Sut): Unit = {
    // Reconnect if we should happen to be disconnected
    // Probably want to have the state available here
    sut.reconnect
    sut.flushdb
    sut.quit
  }

  def genInitialState: Gen[State] = State(
    collection.immutable.Map.empty,
    collection.immutable.Set.empty,
    true
  )

  def initialPreCondition(state: State): Boolean = state.connected

  def newSut(state: State): Sut = new RedisClient(
    "localhost",
    6379
  )

  def genCommand(state: State): Gen[Command] = {
    if(!state.connected) ToggleConnected
    else
      frequency(
        (20, genDel),
        (10, genDelExisting(state)),
        (50, genSet),
        (10, genSetExisting(state)),
        (20, genGet),
        (20, genGetExisting(state)),
        (20, genGetDeleted(state)),
        (20, const(DBSize)),
        ( 1, const(FlushDB)),
        ( 3, const(ToggleConnected))
      )
  }

  //val genKey = arbitrary[String]
  //val genVal = arbitrary[String]
  val genKey = identifier
  val genVal = identifier

  val genSet: Gen[Set] = for {
    key <- genKey
    value <- genVal
  } yield Set(key, value)

  def genDelExisting(state: State): Gen[Del] =
    if(state.contents.isEmpty) genDel
    else someOf(state.contents.keys.toSeq).map(Del.apply)

  def genSetExisting(state: State): Gen[Set] =
    if(state.contents.isEmpty) genSet else for {
      key <- oneOf(state.contents.keys.toSeq)
      value <- oneOf(genVal, const(state.contents(key)))
    } yield Set(key,value)

  val genGet: Gen[Get] = genKey.map(Get.apply)

  val genDel: Gen[Del] = nonEmptyListOf(genKey).map(Del.apply)

  def genGetExisting(state: State): Gen[Get] =
    if(state.contents.isEmpty) genGet else for {
      key <- oneOf(state.contents.keys.toSeq)
    } yield Get(key)

  def genGetDeleted(state: State): Gen[Get] =
    if(state.deleted.isEmpty) genGet else for {
      key <- oneOf(state.deleted.toSeq)
    } yield Get(key)


  case object DBSize extends Command {
    type Result = Option[Long]
    def run(sut: Sut) = sut.dbsize
    def preCondition(state: State) = state.connected
    def nextState(state: State) = state
    def postCondition(state: State, result: Try[Option[Long]]) =
      result == Success(Some(state.contents.keys.size))
  }

  case class Set(key: String, value: String) extends Command {
    type Result = Boolean
    def run(sut: Sut) = sut.set(key, value)
    def preCondition(state: State) = state.connected
    def nextState(state: State) = state.copy(
      contents = state.contents + (key -> value),
      deleted = state.deleted.filter(_ != key)
    )
    def postCondition(state: State, result: Try[Boolean]) =
      result == Success(true)
  }

  case class Del(keys: Seq[String]) extends Command {
    type Result = Option[Long]
    def run(sut: Sut) =
      if(keys.isEmpty) Some(0)
      else sut.del(keys.head, keys.tail.toSeq: _*)
    def preCondition(state: State) = state.connected
    def nextState(state: State) = state.copy(
      contents = state.contents -- keys,
      deleted = state.deleted ++ keys
    )
    def postCondition(state: State, result: Try[Option[Long]]) =
      result == Success(Some(state.contents.filterKeys(keys.contains).size))
  }

  case object FlushDB extends Command {
    type Result = Boolean
    def run(sut: Sut) = sut.flushdb
    def preCondition(state: State) = state.connected
    def nextState(state: State) = state.copy(
      contents = Map.empty
    )
    def postCondition(state: State, result: Try[Boolean]) =
      result == Success(true)
  }

  case object ToggleConnected extends Command {
    type Result = Boolean
    def run(sut: Sut) = {
      if(sut.connected) sut.quit
      else sut.connect
    }
    def preCondition(state: State) = true
    def nextState(state: State) = state.copy(
      connected = !state.connected
    )
    def postCondition(state: State, result: Try[Boolean]) =
      result == Success(true)
  }

  case class Get(key: String) extends Command {
    type Result = Option[String]
    def run(sut: Sut) = sut.get(key)
    def preCondition(state: State) = state.connected
    def nextState(state: State) = state
    def postCondition(state: State, result: Try[Option[String]]) =
      result == Success(state.contents.get(key))
  }

//  case class BitCount(key: String) extends Command {
//    type Result = Option[Int]
//    def run(sut: Sut) = sut.bitcount(key, None)
//    def preCondition(state: State) = state.connected
//    def nextState(state: State) = state
//    def postCondition(state: State, result: Try[Option[Int]]) = {
//      val expected = state.contents.get(key) match {
//        case None => 0
//        case Some(str) bitcount(str)
//      result == Success(state.contents.get(key))
//  }

}
