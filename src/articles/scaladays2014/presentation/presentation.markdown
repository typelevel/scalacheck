class: middle

.left-column[
![](img/logo.svg)
]

.right-column[
# Testing Stateful Systems with ScalaCheck

## Rickard Nilsson

## @scalacheck

## ScalaDays 2014, Berlin
]

---

# PART I

# Very Quick ScalaCheck Intro

# PART II

# ScalaCheck Commands API

# PART III

# ScalaCheck Commands Examples

---

# ScalaCheck is Property-based Testing

```scala
scala> import org.scalacheck.Prop.forAll

scala> val propertyReverseList =
     |   forAll { xs: List[Int] =>
     |     xs.reverse.reverse == xs
     |   }

scala> propertyReverseList.check
+ OK, passed 100 tests.
```

---

# What is a Property?

## "Plus is a commutative operator"
```scala
x + y == y + x
```

## "Compression should make things smaller"
```scala
compress(input).size < input.size
```

## "Round-trip encode/decode"
```scala
decode(encode(input)) == input
```

---

# Shameless Plug - OUT NOW!!

.big-image2[
![](img/book.png)
]

15% discount code: SCALADAYS2014

---

# PART II

# ScalaCheck Commands API

---

# Disclaimers

## Work In Progress

All code in this presentation use ScalaCheck 1.12.0-SNAPSHOT

Feedback and contributions are much appreciated!

## Heavily inspired by Quviq's QuickCheck for Erlang

 * http://www.quviq.com

 * Testing LevelDB - http://goo.gl/q9wfLg

 * Testing Riak - http://goo.gl/FGWXfE

---

# What About State?

```scala
class Database { ... }
```

```scala
val propertyDatabase = ???
```

The contents of a database are the result of a **sequence of state-mutating
commands**.

How can we check that the database behaves correctly?

---

# Specifying Stateful Systems

For ordinary properties, ScalaCheck generates random input values and evaluates
a boolean expression.

---

# Specifying Stateful Systems

For ordinary properties, ScalaCheck generates random input values and evaluates
a boolean expression.

For stateful systems, we can let the input be a sequence of commands, and
evaluate a post-condition for each command.

---

# Specifying Stateful Systems

For ordinary properties, ScalaCheck generates random input values and evaluates
a boolean expression.

For stateful systems, we can let the input be a sequence of commands, and
evaluate a post-condition for each command.

**Can we wrap this up in a simple model that ScalaCheck can understand?**

---

# Modeling Commands

## Simplest possible model

```scala
trait Command {

  def run(): Boolean

}
```

---

# System Under Test

```scala
case class Counter(private var n: Int = 0) {

  def increment(): Int = {
    n = n + 1
    n
  }

  def get: Int = n

}
```

---

# Modeling Commands

```scala
case object Increment extends Command {

  def run(): Boolean = {
    val n = myCounter.increment()
    n == ???
  }

}
```

How can we decide if `increment()` did the right thing? We don't know what the
system looked like before the command ran!

---

# Introducing State

```scala
type State = Long
```

```scala
trait Increment extends Command {
  def run(state: State): Boolean = {
    myCounter.increment() == state + 1
  }

  def nextState(state: State): State = {
    state + 1
  }
}
```

---

# Perfecting the Model

```scala
trait Commands {
  type Sut
  type State

  trait Command {
    type Result
    def run(sut: Sut): Result
    def postCondition(state: State,
      result: Try[Result]): Prop

    def nextState(state: State): State
    def preCondition(state: State): Boolean
  }
  ...
```

---

# Missing pieces

```scala
trait Commands {

  ...

  def newSut(state: State): Sut

  def destroySut(sut: Sut): Unit

  def genInitialState: Gen[State]

  def genCommand(state: State): Gen[Command]

}
```

---

# A Counter Example

```scala
object CounterCommands extends Commands {
  import org.scalacheck.Arbitrary.arbitrary
  import org.scalacheck.Gen.oneOf

  type Sut = Counter
  type State = Long

  def newSut(state: State) = new Counter(state.toInt)

  def genInitialState = arbitrary[Int].map(_.toLong)

  def genCommand(state: State) = oneOf(Increment, Get)
  ...
```

---

# A Counter Example: Increment

```scala
case object Increment extends Command {
  type Result = Int

  def run(counter: Sut) = counter.increment()

  def nextState(s: State) = s + 1

  def preCondition(s: State) = true

  def postCondition(s: State, result: Try[Int]) =
    result == Success(s + 1)
}
```

---

# A Counter Example: Get

```scala
case object Get extends Command {
  type Result = Int

  def run(counter: Sut) = counter.get

  def nextState(s: State) = s

  def preCondition(s: State) = true

  def postCondition(s: State, result: Try[Int]) =
    result == Success(s)
}
```

---

# From State Machine to Property

```scala
val counterProp: Prop = CounterCommands.property()
```

This is a plain ScalaCheck property, that can be tested just like any other
property. All the usual parameters can be used, like `minSuccessfulTests`,
`workers` etc.

On each evaluation of the property, a new `Sut` instance is prepared, and a
random sequence of commands is generated and executed.

---

# From State Machine to Property

```scala
scala> counterProp.check
```

---

# From State Machine to Property

```scala
scala> counterProp.check
! Falsified after 9 passed tests.
> Labels of failing property:
seqcmds =
  (state = 2147483647) (Increment => -2147483648)

> ARG_0: Actions(2147483647,List(Increment),List())

> ARG_0_ORIGINAL: Actions(2147483647,List(Increment,
  Increment, Get, Increment, Get, Get, Increment, Get,
  Get),List())
```

---

# Sweep it under the rug ...

```scala
case object Increment extends Command {

  def preCondition(s: State) = s < Int.MaxValue

  ...

}
```

---

# ... or scream loudly

```scala
case class Counter(private var n: Int = 0) {
  def increment() =
    if (n < Int.MaxValue) n = n + 1; n
    else throw new Exception
}

case object Increment extends Command {
  def nextState(s: State) =
    if (s == Int.MaxValue) s else s + 1

  def postCondition(s: State, result: Try[Int]) =
    if (s >= Int.MaxValue) result.isFailure
    else result == Success(s + 1)
}
```

---

# Either way

```scala
scala> counterProp.check
+ OK, passed 100 tests.
```

---

# Testing Thread Safety

```scala
val counterProp: Prop = CounterCommands.property(
  threadCount = 3
)
```

---

# Testing Thread Safety

```scala
scala> counterProp.check
```

---

# Testing Thread Safety

```scala
scala> counterProp.check
! Falsified after 6 passed tests.
> Labels of failing property:
initialstate = 0
seqcmds = (Get => 0; Increment => 1; Increment => 2;
  Increment => 3; Increment => 4; Increment => 5)
parcmds =
(Increment => 6; Increment => 7; Increment => 8; Get
   => 8; Get => 8; Get => 8; Increment => 9),
(Get => 9; Get => 9; Increment => 10; Increment => 11;
  Get => 11; Get => 11; Increment => 12; Get => 12),
(Get => 11; Increment => 12; Get => 12; Increment =>
  13; Increment => 14; Get => 14; Increment => 15;
  Increment => 16)
```

---

# Testing Thread Safety

`Increment` ran 17 times, but the highest return value was 16!

We have a race condition in our `Counter` code:

```scala
def increment(): Int = { n = n + 1; n }
```

We didn't change anything in our state machine to find this bug, we just
bumped the number of threads ScalaCheck uses to run the commands.

**How does it work?**

---

# Sequential Phase

```scala
Get => 0; Increment => 1; Increment => 2;
  Increment => 3; Increment => 4; Increment => 5
```

Postconditions are checked after each command run.

In each step, a new state is calculated with `Command.nextState()` and fed to
the `postCondition()` method of the next command.

---

# Parallel Phase

Three command sequences run in parallel.

```scala
Increment => 6; Increment => 7; Increment => 8; Get
   => 8; Get => 8; Get => 8; Increment => 9

Get => 9; Get => 9; Increment => 10; Increment => 11;
  Get => 11; Get => 11; Increment => 12; Get => 12

Get => 11; Increment => 12; Get => 12; Increment =>
  13; Increment => 14; Get => 14; Increment => 15;
  Increment => 16
```

We can't check any postconditions since the commands might be interleaved in
any way.

---

# Parallel Phase

.big-image[
![](img/drawing.svg)
]

---

# Parallel Phase

We calculate all possible command transitions, and the possible end states
they would result in.

The final command that runs in the parallel phase must be the last command
of one of the threads.

Therefore, we can check the post condition of each possible last command
against each possible last state.

---

# PART III

# ScalaCheck Commands Examples

---

# Testing Redis

## Modeling the state

```scala
case class State (
  contents: Map[String,String],
  deleted: Set[String],
  connected: Boolean
)
```


## System Under Test

```scala
type Sut = RedisClient
```

---

# Redis Commands example: Get

```scala
case class Get(key: String) extends Command {
  type Result = Option[String]

  def run(sut: Sut) = sut.get(key)

  def preCondition(s: State) = s.connected

  def nextState(s: State) = s

  def postCondition(s: State, r: Try[Option[String]]) =
    r == Success(s.contents.get(key))
}
```

---

# Redis Commands example: Get

```scala
val genGet: Gen[Get] = genKey.map(Get)

def genGetExisting(state: State): Gen[Get] =
  if(state.contents.isEmpty) genGet
  else for {
    key <- oneOf(state.contents.keys.toSeq)
  } yield Get(key)

def genGetDeleted(state: State): Gen[Get] =
  if(state.deleted.isEmpty) genGet
  else for {
    key <- oneOf(state.deleted.toSeq)
  } yield Get(key)
```

---

# Redis Commands example: Delete

```scala
case class Del(keys: Seq[String]) extends Command {
  type Result = Option[Long]

  def run(sut: RedisClient) =
    if(keys.isEmpty) Some(0)
    else sut.del(keys.head, keys.tail: _*)

  def nextState(s: State) = s.copy(
    contents = s.contents -- keys,
    deleted = s.deleted ++ keys
  )
  def postCondition(s: State, r: Try[Option[Long]]) = {
    val n = s.contents.filterKeys(keys.contains).size
    r == Success(Some(n))
  }
```

---

# Redis Commands example: Delete

```scala
val genDel: Gen[Del] = nonEmptyListOf(genKey).map(Del)

def genDelExisting(state: State): Gen[Del] =
  if(state.contents.isEmpty) genDel
  else someOf(state.contents.keys.toSeq).map(Del)

def genDelDeleted(state: State): Gen[Del] =
  if(state.deleted.isEmpty) genDel
  else someOf(state.deleted.toSeq).map(Del)

def genDelAll(state: State): Gen[Del] =
  const(Del(state.contents.keys.toSeq))
```

---

# Testing Redis

## It can make sense to model things that is not really part of the system state, like deleted keys

## Implement generic commands and specialised generators

---

# Testing Redis

## Example project in ScalaCheck's repository

examples/commands-redis [http://goo.gl/81jIXS]

## Implemented Commands

```scala
DBSize, FlushDB, Del, Get, Set, Disconnect, Connect
```

---

# Testing Stateful Server Networks

## Idea

Let ScalaCheck generate a network of virtual machines and execute a sequence of
commands that verifies the overall system functionality

## Proof of concept project in ScalaCheck's repository

examples/commands-nix [http://goo.gl/LSe4jb]

---

# Generating Virtual Machines

## Introducing NixOS

[NixOS](http://nixos.org) is a Linux distribution based on the functional build
tool and package manager Nix. It lets you define servers in a declarative
fashion.

```
{
  boot.loader.grub.device = "/dev/sda";

  fileSystems."/".device = "/dev/sda1";

  services.sshd.enable = true;
}
```

---

# Defining a Network State

```scala
type State = List[Machine]

case class Machine (
  id: String,
  uuid: java.util.UUID,
  ip: String,
  kernelVer: String,
  memory: Int,
  running: Boolean
)
```

---

# Defining a Network State

```scala
def genMachine(id: String): Gen[Machine] = for {

  uuid <- Gen.uuid

  ip <- Gen.choose(2,254).map(n => s"172.16.2.$n")

  memory <- Gen.choose(96, 256)

  kernel <- Gen.oneOf("3.14", "3.13", "3.12", "3.10")

} yield Machine(
  id, uuid, ip, kernel, memory, false
)
```

---

# The System Under Test

[libvirt](http://libvirt.org) and [QEMU](http://qemu.org) manage the virtual
machines.

```scala
type Sut = Map[String, org.libvirt.Domain]
```

```scala
def newSut(state: State): Sut = {
  val con = new org.libvirt.Connect("qemu:///session")
  toLibvirtXMLs(state) map { case (id,xml) =>
    id -> con.domainDefineXML(xml)
  }
}
```

`toLibvirtXMLs` create NixOS configs for all machines, and then calls out to
`nix-build` to build XML-files that `libvirt` can use.


---

# Generating NixOS configuration

```scala
def toNixMachine(m: Machine): String = raw"""
  deployment.libvirt = {
    netdevs.netdev0.mac = "$$MAC0";
    memory = ${m.memory};
    uuid = "${m.uuid}";
  };
  networking.hostName = "${m.id}";
  networking.interfaces.eth0 = {
    ipAddress = "${m.ip}";
    prefixLength = 24;
  };
  boot.kernelPackages =
    pkgs.linuxPackages_${m.kernelVer.replace('.','_')};
"""
```

---

# A Ping Command

```scala
case class Ping(from: Machine, to: Machine) {
  type Result = Boolean

  def run(sut: Sut) =
    runSshCmd(from.ip, s"fping -c 1 ${to.ip}") match {
      case Right(_) => true; case Left(_) => false
    }

  def nextState(s: State) = s
  def preCondition(s: State) = from.running

  def postCondition(s: State, r: Try[Boolean]) =
    r == Success(to.running)
}
```

---

# Testing Stateful Server Networks

## The generated network could be nodes in a distributed database. Or an actor system. Or any server system.

## NixOS allows for true integration tests. Any component of the complete system can be turned into a parameter in the state model.

## More work needed to package this in a useable format.

---

# Wrap Up

## Property-based testing can be applied to more than immutable functions!

## The Commands API needs more evaluation and polishing. Will land in ScalaCheck 1.12.0

## ScalaCheck can be a core engine in a custom testing setup

## ScalaCheck + NixOS is an interesting fit

---

# Questions?
