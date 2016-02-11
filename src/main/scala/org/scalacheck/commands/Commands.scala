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
import scala.util.{Try, Success, Failure}
import scala.language.implicitConversions

/** An API for stateful testing in ScalaCheck.
 *
 *  For an implementation overview, see the examples in ScalaCheck's source tree.
 *
 *  @since 1.12.0
 */
trait Commands {
/**
    *  The [[Term]] type models a (per test) unique binding, and a possible
    *  value. Symbolic terms are meant for use during test generation, and
    *  dynamic terms are for runtime. ScalaCheck automatically encapsulates the
    *  results of a command into SymbolicTerm's during test generation, and DynamicTerm's
    *  during runtime.
    */
  sealed abstract class Term[A](val binding: Binding) {
    def get: Try[A]
    def isEmpty: Boolean
    def isDefined: Boolean = !isEmpty
    final def nonEmpty = isDefined
    final def getOrElse[B >: A](default: => B): B = if (isEmpty || get.isFailure) default else this.get.get
    final def orNull[A1 >: A](implicit ev: Null <:< A1): A1 = this getOrElse ev(null)
    
    // Returns result of applying $f to this $term's value if
    // the term is non-empty and has a Success value. Otherwise,
    // evaluates ifEmpty.
    final def fold[B](ifEmpty: => B)(f: A => B): B = if(isEmpty) ifEmpty else f(get.get)
    
    final def map[B](f: A => B): Option[B] = {
      if(isEmpty) None else Some(f(get.get))
    }
    
    final def flatMap[B](f: A => Option[B]): Option[B] = {
      if(isEmpty) None else f(get.get)
    }
    
    // Will return Some(value), if there is a Success value and p is true. Otherwise None
    final def filter(p: A => Boolean): Option[A] =
      if(isEmpty) None else if(p(get.get)) Some(get.get) else None
    
    // Filters based on the binding, not the value.
    final def filterBinding(p: Binding => Boolean): Option[Term[A]] =
      if(p(this.binding)) Some(this) else None
    
    final def contains[A1 >: A](elem: A1): Boolean =
      !isEmpty && get.get == elem
    
    final def exists(p: A => Boolean): Boolean =
      !isEmpty && p(get.get)
    
    final def forall(p: A => Boolean): Boolean = isEmpty || p(get.get)
    
    final def foreach[U](f: A => U) {
        if(!isEmpty) f(get.get)
      }
    
    final def collect[B](pf: PartialFunction[A, B]): Option[B] =
      if(!isEmpty) pf.lift(this.get.get) else None
      
    final def orElse[B >: A](alternative: => Option[B]): Option[B] =
      if(isEmpty) alternative else Some(this.get.get)
  }
  
   /**
   * A Binding is a (per SUT) unique identifier, basically a wrapped int with a nicer name. This helps clarify
   * intent, if you need to keep track of term bindings in your State, e.g. a Map[String, Binding]
   * has the clear intent of mapping strings to a term binding, while Map[String, Int] is less clear.
   */
  case class Binding(val binding: Int)
  implicit def intToBinding(o: Int) = new Binding(o)
  
  case class SymbVar[A](override val binding: Binding) extends Term[A](binding) {
    override def isEmpty = true
    override def get = throw new NoSuchElementException("SymbVar.get")
  }
  
  case class DynVar[A](override val binding: Binding, val value: Try[A]) extends Term[A](binding) {
    override def isEmpty = get.isFailure
    override def get = value
  }

  class OptionTerm[A](opt: Option[Term[A]]) {
    def valueOrElse[B >: A](a: B): B = valueOpt getOrElse a
    def valueOpt: Option[A] = opt flatMap { term => term map { v => v } }
  }

  implicit def optionToTermOption[A](o: Option[Term[A]]) = new OptionTerm[A](o)
  /** The abstract state type. Must be immutable.
   *  The [[State]] type should model the state of the system under
   *  test (SUT). It should only contain details needed for specifying
   *  our pre- and postconditions, and for creating [[Sut]]
   *  instances. */
  type State

  /** A type representing one instance of the system under test (SUT).
   *  The [[Sut]] type should be a proxy to the actual system under
   *  test and is therefore, by definition, a mutable type.
   *  It is used by the [[Command.run]] method to execute commands in the
   *  system under test. It should be possible to have any
   *  number of co-existing instances of the [[Sut]] type, as long as
   *  [[canCreateNewSut]] isn't violated, and each [[Sut]]
   *  instance should be a proxy to a distinct SUT instance. There should be no
   *  dependencies between the [[Sut]] instances, as they might be used
   *  in parallel by ScalaCheck. [[Sut]] instances are created by
   *  [[newSut]] and destroyed by
   *  [[destroySut]]. [[newSut]] and
   *  [[destroySut]] might be called at any time by
   *  ScalaCheck, as long as [[canCreateNewSut]] isn't violated. */
  type Sut

  /** Decides if [[newSut]] should be allowed to be called
   *  with the specified state instance. This can be used to limit the number
   *  of co-existing [[Sut]] instances. The list of existing states represents
   *  the initial states (not the current states) for all [[Sut]] instances
   *  that are active for the moment. If this method is implemented
   *  incorrectly, for example if it returns false even if the list of
   *  existing states is empty, ScalaCheck might hang.
   *
   *  If you want to allow only one [[Sut]] instance to exist at any given time
   *  (a singleton [[Sut]]), implement this method the following way:
   *
   *  {{{
   *  def canCreateNewSut(newState: State, initSuts: Traversable[State]
   *    runningSuts: Traversable[Sut]
   *  ) = {
   *    initSuts.isEmpty && runningSuts.isEmpty
   *  }
   *  }}}
   */
  def canCreateNewSut(newState: State, initSuts: Traversable[State],
    runningSuts: Traversable[Sut]): Boolean

  /** Create a new [[Sut]] instance with an internal state that
   *  corresponds to the provided abstract state instance. The provided state
   *  is guaranteed to fulfill [[initialPreCondition]], and
   *  [[newSut]] will never be called if
   *  [[canCreateNewSut]] is not true for the given state. */
  def newSut(state: State): Sut

  /** Destroy the system represented by the given [[Sut]]
   *  instance, and release any resources related to it. */
  def destroySut(sut: Sut): Unit

  /** The precondition for the initial state, when no commands yet have
   *  run. This is used by ScalaCheck when command sequences are shrinked
   *  and the first state might differ from what is returned from
   *  [[genInitialState]]. */
  def initialPreCondition(state: State): Boolean

  /** A generator that should produce an initial [[State]] instance that is
   *  usable by [[newSut]] to create a new system under test.
   *  The state returned by this generator is always checked with the
   *  [[initialPreCondition]] method before it is used. */
  def genInitialState: Gen[State]

  /** A generator that, given the current abstract state, should produce
   *  a suitable Command instance. */
  def genCommand(state: State): Gen[Command]

  /** A type representing the commands that can run in the system under test.
   *  This type should be immutable and implement the equality operator
   *  properly. */
  trait Command {
    /** An abstract representation of the result of running this command in
     *  the system under test. The [[Result]] type should be immutable
     *  and it should encode everything about the command run that is necessary
     *  to know in order to correctly implement the
     *  [[[Command!.postCondition* postCondition]]] method. */
    type Result

    /** Executes the command in the system under test, and returns a
     *  representation of the result of the command run. The result value
     *  is later used for verifying that the command behaved according
     *  to the specification, by the [[Command!.postCondition* postCondition]]
     *  method. */
    def run(sut: Sut, state: State): Result

    /** Returns a new [[State]] instance that represents the
     *  state of the system after this command has run, given the system
     *  was in the provided state before the run. */
    def nextState(state: State, v: Term[Result]): State

    /** Precondition that decides if this command is allowed to run
     *  when the system under test is in the provided state. */
    def preCondition(state: State): Boolean

    /** Postcondition that decides if this command produced the correct result
     *  or not, given the system was in the provided state before the command
     *  ran. */
    def postCondition(state: State, result: Try[Result]): Prop

    /** Wraps the run and postCondition methods in order not to leak the
     *  dependant Result type. */
    private[Commands] def runPC(sut: Sut, state: State): (Try[String], State => Prop) = {
      import Prop.BooleanOperators
      val r = Try(run(sut, state))
      (r.map(_.toString), s => preCondition(s) ==> postCondition(s,r))
    }
  }

  /** A command that never should throw an exception on execution. */
  trait SuccessCommand extends Command {
    def postCondition(state: State, result: Result): Prop
    final override def postCondition(state: State, result: Try[Result]) =
      result match {
        case Success(result) => postCondition(state, result)
        case Failure(e) => Prop.exception(e)
      }
  }

  /** A command that doesn't return a result, only succeeds or fails. */
  trait UnitCommand extends Command {
    final type Result = Unit
    def postCondition(state: State, success: Boolean): Prop
    final override def postCondition(state: State, result: Try[Unit]) =
      postCondition(state, result.isSuccess)
  }

  /** A command that doesn't do anything */
  case object NoOp extends Command {
    type Result = Null
    def run(sut: Sut, state: State) = null
    def nextState(state: State, v: Term[Null]) = state
    def preCondition(state: State) = true
    def postCondition(state: State, result: Try[Null]) = true
  }

  /** A property that can be used to test this [[Commands]] specification.
   *
   *  The parameter `threadCount` specifies the number of commands that might
   *  be executed in parallel. Defaults to one, which means the commands will
   *  only be run serially for the same [[Sut]] instance. Distinct [[Sut]]
   *  instances might still receive commands in parallel, if the
   *  [[Test.Parameters.workers]] parameter is larger than one. Setting
   *  `threadCount` higher than one enables ScalaCheck to reveal
   *  thread-related issues in your system under test.
   *
   *  When setting `threadCount` larger than one, ScalaCheck must evaluate
   *  all possible command interleavings (and the end [[State]] instances
   *  they produce), since parallel command execution is non-deterministic.
   *  ScalaCheck tries out all possible end states with the
   *  [[Command.postCondition]] function of the very last command executed
   *  (there is always exactly one command executed after all parallel command
   *  executions). If it fails to find an end state that satisfies the
   *  postcondition, the test fails.
   *  However, the number of possible end states grows rapidly with increasing
   *  values of `threadCount`. Therefore, the lengths of the parallel command
   *  sequences are limited so that the number of possible end states don't
   *  exceed `maxParComb`. The default value of `maxParComb` is 1000000. */
  final def property(threadCount: Int = 1, maxParComb: Int = 1000000): Prop = {
    val suts = collection.mutable.Map.empty[AnyRef,(State,Option[Sut])]

    Prop.forAll(actions(threadCount, maxParComb)) { as =>
      try {
        val sutId = suts.synchronized {
          val initSuts = for((state,None) <- suts.values) yield state
          val runningSuts = for((_,Some(sut)) <- suts.values) yield sut
          if (canCreateNewSut(as.s, initSuts, runningSuts)) {
            val sutId = new AnyRef
            suts += (sutId -> (as.s,None))
            Some(sutId)
          } else None
        }
        sutId match {
          case Some(id) =>
            val sut = newSut(as.s)
            def removeSut  {
              suts.synchronized {
                suts -= id
                destroySut(sut)
              }
            }
            val doRun = suts.synchronized {
              if (suts.contains(id)) {
                suts += (id -> (as.s,Some(sut)))
                true
              } else false
            }
            if (doRun) runActions(sut,as, removeSut)
            else {
              removeSut
              Prop.undecided
            }

          case None => // NOT IMPLEMENTED Block until canCreateNewSut is true
            println("NOT IMPL")
            Prop.undecided
        }
      } catch { case e: Throwable =>
        suts.synchronized { suts.clear }
        throw e
      }
    }
  }

  /** Override this to provide a custom Shrinker for your internal
    * [[State]].  By default no shrinking is done for [[State]]. */
  def shrinkState: Shrink[State] = implicitly

  // Private methods //
  private type Commands = List[Command]

  private case class Actions(
    s: State, seqCmds: Commands, parCmds: List[Commands]
  )

  private implicit val shrinkActions = Shrink[Actions] { as =>
    val shrinkedCmds: Stream[Actions] =
      Shrink.shrink(as.seqCmds).map(cs => as.copy(seqCmds = cs)) append
      Shrink.shrink(as.parCmds).map(cs => as.copy(parCmds = cs))

    Shrink.shrinkWithOrig[State](as.s)(shrinkState) flatMap { state =>
      shrinkedCmds.map(_.copy(s = state))
    }
  }

  private def runSeqCmds(sut: Sut, s0: State, cs: Commands
  ): (Prop, State, List[Try[String]]) = {
    val (prop, finalState, labels, _) = cs.foldLeft((Prop.proved,s0,List[Try[String]](),1)) { 
      case ((p,s,rs,count),c) => {
        import Prop.BooleanOperators
        val r = Try(c.run(sut, s))
        val pf:State => Prop = st => c.preCondition(st) ==> c.postCondition(st,r)
        val term = DynVar(Binding(count), r)
        (p && pf(s), c.nextState(s,term), rs :+ r.map(_.toString), count + 1)
      }
    }
    (prop, finalState, labels)
    }

  private def runParCmds(sut: Sut, s: State, pcmds: List[Commands]
  ): (Prop, List[List[(Command,Try[String])]]) = {
    import concurrent._
    val tp = java.util.concurrent.Executors.newFixedThreadPool(pcmds.size)
    implicit val ec = ExecutionContext.fromExecutor(tp)
    val memo = collection.mutable.Map.empty[(State,List[Commands]), List[State]]

    def endStates(scss: (State, List[Commands])): List[State] = {
      val (s,css) = (scss._1, scss._2.filter(_.nonEmpty))
      var count = 0
      (memo.get((s,css)),css) match {
        case (Some(states),_) => states
        case (_,Nil) => List(s)
        case (_,cs::Nil) =>
          List(cs.init.foldLeft(s) { case (s0,c) => {
            count += 1
            c.nextState(s0, SymbVar(count)) 
          }})
        case _ =>
          val inits = scan(css) { case (cs,x) => {
            count += 1
            (cs.head.nextState(s, SymbVar(count)), cs.tail::x)
          }}
          val states = inits.distinct.flatMap(endStates).distinct
          memo += (s,css) -> states
          states
      }
    }

    def run(endStates: List[State], cs: Commands
    ): Future[(Prop,List[(Command,Try[String])])] = Future {
      if(cs.isEmpty) (Prop.proved, Nil) else blocking {
        val rs = cs.init.map(_.runPC(sut, s)._1)
        val (r,pf) = cs.last.runPC(sut, s)
        (Prop.atLeastOne(endStates.map(pf): _*), cs.zip(rs :+ r))
      }
    }

    try {
      val res = Future.traverse(pcmds)(run(endStates(s,pcmds), _)) map { l =>
        val (ps,rs) = l.unzip
        (Prop.atLeastOne(ps: _*), rs)
      }
      Await.result(res, concurrent.duration.Duration.Inf)
    } finally { tp.shutdown() }
  }

  /** Formats a list of commands with corresponding results */
  private def prettyCmdsRes(rs: List[(Command,Try[String])]) = {
    val cs = rs.map {
      case (c, Success("()")) => c.toString
      case (c, Success(r)) => s"$c => $r"
      case (c,r) => s"$c => $r"
    }
    cs.mkString("(","; ",")")
  }

  /** A property that runs the given actions in the given SUT */
  private def runActions(sut: Sut, as: Actions, finalize : =>Unit): Prop = {
    try{
    val (p1, s, rs1) = runSeqCmds(sut, as.s, as.seqCmds)
    val l1 = s"initialstate = ${as.s}\nseqcmds = ${prettyCmdsRes(as.seqCmds zip rs1)}"
    if(as.parCmds.isEmpty) p1 :| l1
    else propAnd(p1.flatMap{r => if(!r.success) finalize; Prop(prms => r)} :| l1, {
      try{
      val (p2, rs2) = runParCmds(sut, s, as.parCmds)
      val l2 = rs2.map(prettyCmdsRes).mkString("(",",\n",")")
      p2 :| l1 :| s"parcmds = (state = ${s}) $l2"
      }
      finally finalize
    })
    }
    finally if(as.parCmds.isEmpty) finalize
  }

  /** [[Actions]] generator */
  private def actions(threadCount: Int, maxParComb: Int): Gen[Actions] = {
    import Gen.{const, listOfN, choose, sized}

    def sizedCmds(s: State)(sz: Int): Gen[(State,Commands)] = {
      val l: List[Unit] = List.fill(sz)(())
      var count = 0
      l.foldLeft(const((s,Nil:Commands))) { case (g,()) =>
        for {
          (s0,cs) <- g
          c <- genCommand(s0) suchThat (_.preCondition(s0))
        } yield {
          count += 1
          (c.nextState(s0, SymbVar(count)), cs :+ c)
        }
      }
    }

    def cmdsPrecond(s: State, cmds: Commands, count: Int): (State,Boolean) = cmds match {
      case Nil => (s,true)
      case c::cs if c.preCondition(s) => cmdsPrecond(c.nextState(s, SymbVar(count)), cs, count + 1)
      case _ => (s,false)
    }

    def actionsPrecond(as: Actions) =
      as.parCmds.length != 1 && as.parCmds.forall(_.nonEmpty) &&
      initialPreCondition(as.s) && (cmdsPrecond(as.s, as.seqCmds, 1) match {
        case (s,true) => as.parCmds.forall(cmdsPrecond(s,_,1)._2)
        case _ => false
      })

    // Number of sequences to test (n = threadCount, m = parSz):
    //   2^m * 3^m * ... * n^m
    //
    // def f(n: Long, m: Long): Long =
    //   if(n == 1) 1
    //   else math.round(math.pow(n,m)) * f(n-1,m)
    //
    //    m  1     2       3         4           5
    // n 1   1     1       1         1           1
    //   2   2     4       8        16          32
    //   3   6    36     216      1296        7776
    //   4  24   576   13824    331776     7962624
    //   5 120 14400 1728000 207360000 24883200000

    val parSz = {
      // Nbr of combinations
      def seqs(n: Long, m: Long): Long =
        if(n == 1) 1 else math.round(math.pow(n,m)) * seqs(n-1,m)

      if (threadCount < 2) 0 else {
        var parSz = 1
        while (seqs(threadCount, parSz) < maxParComb) parSz += 1
        parSz
      }
    }

    val g = for {
      s0 <- genInitialState
      (s1,seqCmds) <- sized(sizedCmds(s0))
      parCmds <- if(parSz <= 0) const(Nil) else
                 listOfN(threadCount, sizedCmds(s1)(parSz).map(_._2))
    } yield Actions(s0, seqCmds, parCmds)

    g.suchThat(actionsPrecond)
  }

  /** [1,2,3] -- [f(1,[2,3]), f(2,[1,3]), f(3,[1,2])] */
  private def scan[T,U](xs: List[T])(f: (T,List[T]) => U): List[U] = xs match {
    case Nil => Nil
    case y::ys => f(y,ys) :: scan(ys) { case (x,xs) => f(x,y::xs) }
  }

  /** Short-circuit property AND operator. (Should maybe be in Prop module) */
  private def propAnd(p1: => Prop, p2: => Prop) = p1.flatMap { r =>
    if(r.success) Prop.secure(p2) else Prop(prms => r)
  }

}
