# ScalaCheck User Guide

## What is ScalaCheck?

ScalaCheck is a tool for testing Scala and Java programs, based on property
specifications and automatic test data generation. The basic idea is that you
define a property that specifies the behaviour of a method or some unit of
code, and ScalaCheck checks that the property holds. All test data are
generated automatically in a random fashion, so you don't have to worry about
any missed cases.

## A quick example

Fire up the Scala interpreter, with ScalaCheck in the classpath.
```bash
$ scala -cp scalacheck.jar
```
Import the `forAll` method, which creates universally quantified properties. We
will dig into the different property methods later on, but `forAll` is probably
the one you will use the most.

```
scala> import org.scalacheck.Prop.forAll
```

Define a property.

```
scala> val propConcatLists = forAll { (l1: List[Int], l2: List[Int]) =>
  l1.size + l2.size == (l1 ::: l2).size }
```

Check the property!

```
scala> propConcatLists.check
+ OK, passed 100 tests.
```

OK, that seemed alright. Now define another property.

```
scala> val propSqrt = forAll { (n: Int) => scala.math.sqrt(n*n) == n }
```

Check it!

```
scala> propSqrt.check
! Falsified after 2 passed tests.
> ARG_0: -1
> ARG_0_ORIGINAL: -488187735
```

Not surprisingly, the property doesn't hold. The argument `-1` falsifies it.
You can also see that the argument `-488187735` falsifies the property. That
was the first argument ScalaCheck found, and it was then simplified to `-1`.
You'll read more about this later on.

## Concepts

### Properties

A *property* is the testable unit in ScalaCheck, and is represented by the
`org.scalacheck.Prop` class. There are several ways to create properties in
ScalaCheck, one of them is to use the `org.scalacheck.Prop.forAll` method
like in the example above. That method creates universally quantified properties
directly, but it is also possible to create new properties by combining other
properties, or to use any of the specialised methods in the `org.scalacheck.Prop`
object.

#### Universally quantified properties

As mentioned before, `org.scalacheck.Prop.forAll` creates universally
quantified properties. `forAll` takes a function as parameter, and creates a
property out of it that can be tested with the `check` method.  The function
should return `Boolean` or another property, and can take parameters of any
types, as long as there exist implicit `Arbitrary` instances for the types. By
default, ScalaCheck has instances for common types like `Int`, `String`,
`List`, etc, but it is also possible to define your own `Arbitrary` instances.
This will be described in a later section.

Here are some examples of properties defined with help of the
`org.scalacheck.Prop.forAll` method.

```scala
import org.scalacheck.Prop.forAll

val propReverseList = forAll { (l: List[String]) => l.reverse.reverse == l }

val propConcatString = forAll { (s1: String, s2: String) =>
  (s1 + s2).endsWith(s2)
}
```
When you run `check` on the properties, ScalaCheck generates random
instances of the function parameters and evaluates the results, reporting any
failing cases.

You can also give `forAll` a specific data generator. See the following
example:

```scala
import org.scalacheck._

val smallInteger = Gen.choose(0,100)

val propSmallInteger = Prop.forAll(smallInteger) { n =>
  n >= 0 && n <= 100
}
```

`smallInteger` defines a *generator* that generates integers between 0 and 100,
inclusively. Generators will be described closer in a later section.
`propSmallInteger` simply specifies that each integer generated should be in
the correct range. This way of using the `forAll` method is good to use when
you want to control the data generation by specifying exactly which generator
that should be used, and not rely on a default generator for the given type.

#### Conditional Properties

Sometimes, a specification takes the form of an implication. In ScalaCheck,
you can use the implication operator `==>`:

```scala
import org.scalacheck.Prop.{forAll, propBoolean}

val propMakeList = forAll { (n: Int) =>
  (n >= 0 && n < 10000) ==> (List.fill(n)("").length == n)
}
```

Now ScalaCheck will only care for the cases when `n` is not negative. We
also filter out large numbers, since we don't want to generate huge lists.

If the implication operator is given a condition that is hard or impossible to
fulfill, ScalaCheck might not find enough passing test cases to state that the
property holds. In the following trivial example, all cases where `n` is
non-zero will be thrown away:

```
scala> import org.scalacheck.Prop.{forAll, propBoolean}

scala> val propTrivial = forAll { (n: Int) =>
     |  (n == 0) ==> (n == 0)
     | }

scala> propTrivial.check
! Gave up after only 4 passed tests. 500 tests were discarded.
```

It is possible to tell ScalaCheck to try harder when it generates test cases,
but generally you should try to refactor your property specification instead of
generating more test cases, if you get this scenario.

Using implications, we realise that a property might not just pass or fail, it
could also be undecided if the implication condition doesn't get fulfilled. In
the section about test execution, the different results of property evaluations
will be described in more detail.

#### Combining Properties

A third way of creating properties, is to combine existing properties into new ones.

```scala
val p1 = forAll(...)

val p2 = forAll(...)

val p3 = p1 && p2

val p4 = p1 || p2

val p5 = p1 == p2

val p6 = all(p1, p2) // same as p1 && p2

val p7 = atLeastOne(p1, p2) // same as p1 || p2
```

Here, `p3` will hold if and only if both `p1` and `p2` hold, `p4` will hold if
either `p1` or `p2` holds, and `p5` will hold if `p1` holds exactly when `p2`
holds and vice versa.


#### Grouping properties

Often you want to specify several related properties, perhaps for all methods
in a class. ScalaCheck provides a simple way of doing this, through the
`Properties` trait. Look at the following specifications of some of the
methods in the `java.lang.String` class:

```scala
import org.scalacheck._

object StringSpecification extends Properties("String") {
  import Prop.forAll

  property("startsWith") = forAll { (a: String, b: String) =>
    (a+b).startsWith(a)
  }

  property("endsWith") = forAll { (a: String, b: String) =>
    (a+b).endsWith(b)
  }

  property("substring") = forAll { (a: String, b: String) =>
    (a+b).substring(a.length) == b
  }

  property("substring") = forAll { (a: String, b: String, c: String) =>
    (a+b+c).substring(a.length, a.length+b.length) == b
  }
}
```
The `Properties` class contains a `main` method that can be used for simple
execution of the property tests. Compile and run the tests in the
following way:

```bash
$ scalac -cp scalacheck.jar StringSpecification.scala

$ scala -cp scalacheck.jar:. StringSpecification
+ String.startsWith: OK, passed 100 tests.
+ String.endsWith: OK, passed 100 tests.
+ String.substring: OK, passed 100 tests.
+ String.substring: OK, passed 100 tests.
```

You can also use the `check` method of the `Properties` class to
check all specified properties, just like for simple `Prop` instances.
In fact, `Properties` is a subtype of `Prop`, so you can use it just as if
it was a single property. That single property holds if and only if all
of the contained properties hold.

There is a `Properties.include` method you can use if you want to
group several different property collections into a single one. You
could for example create one property collection for your application
that consists of all the property collections of your individual classes:

```scala
object MyAppSpecification extends Properties("MyApp") {
  include(StringSpecification)
  include(...)
  include(...)
}
```

#### Labeling Properties

Sometimes it can be difficult to decide exactly what is wrong when a property
fails, especially if the property is complex, with many conditions. In such
cases, you can label the different parts of the property, so ScalaCheck can
tell you exactly what part is failing. Look at the following example, where
the different conditions of the property have been labeled differently:

```scala
import org.scalacheck.Prop.{forAll, propBoolean}

val complexProp = forAll { (m: Int, n: Int) =>
  val res = myMagicFunction(n, m)
  (res >= m)    :| "result > #1" &&
  (res >= n)    :| "result > #2" &&
  (res < m + n) :| "result not sum"
}
```

We can see the label if we define `myMagicFunction` incorrectly and then
check the property:

```
scala> def myMagicFunction(n: Int, m: Int) = n + m
myMagicFunction: (Int,Int)Int

scala> complexProp.check
! Falsified after 0 passed tests.
> Label of failing property: "result not sum"
> ARG_0: "0"
> ARG_1: "0"
```

It is also possible to write the label before the conditions like this:

```scala
import org.scalacheck.Prop.{forAll, propBoolean}

val complexProp = forAll { (m: Int, n: Int) =>
  val res = myMagicFunction(n, m)
  ("result > #1"    |: res >= m) &&
  ("result > #2"    |: res >= n) &&
  ("result not sum" |: res < m + n)
}
```

The labeling operator can also be used to inspect intermediate values
used in the properties, which can be very useful when trying to understand
why a property fails. ScalaCheck always presents the generated property
arguments (`ARG_0`, `ARG_1`, etc), but sometimes you need to quickly see
the value of an intermediate calculation. See the following example, which
tries to specify multiplication in a somewhat naive way:

```scala
import org.scalacheck.Prop.{forAll, propBoolean, all}

val propMul = forAll { (n: Int, m: Int) =>
  val res = n*m
  ("evidence = " + res) |: all(
    "div1" |: m != 0 ==> (res / m == n),
    "div2" |: n != 0 ==> (res / n == m),
    "lt1"  |: res > m,
    "lt2"  |: res > n
  )
}
```

Here we have four different conditions, each with its own label. Instead
of using the `&&` operator the conditions are combined in an equivalent
way by using the `Prop.all` method. The implication operators are used to
protect us from zero-divisions. A fifth label is added to the combined
property to record the result of the multiplication. When we check the
property, ScalaCheck tells us the following:

```
scala> propMul.check
! Falsified after 0 passed tests.
> Labels of failing property:
"lt1"
"evidence = 0"
> ARG_0: "0"
> ARG_1: "0"
```

As you can see, you can add as many labels as you want to your property,
ScalaCheck will present them all if the property fails.

### Generators

Generators are responsible for generating test data in ScalaCheck, and are
represented by the `org.scalacheck.Gen` class. You need to know how to use this
class if you want ScalaCheck to generate data of types that are not supported
by default, or if you want to use the `forAll` method mentioned above, to state
properties about a specific subset of a type. In the `Gen` object, there are
several methods for creating new and modifying existing generators. We will
show how to use some of them in this section. For a more complete reference of
what is available, please see the API scaladoc.

A generator can be seen simply as a function that takes some generation
parameters, and (maybe) returns a generated value. That is, the type `Gen[T]`
may be thought of as a function of type `Gen.Params => Option[T]`. However, the
`Gen` class contains additional methods to make it possible to map generators,
use them in for-comprehensions and so on. Conceptually, though, you should
think of generators simply as functions, and the combinators in the `Gen`
object can be used to create or modify the behaviour of such generator
functions.

Let's see how to create a new generator. The best way to do it is to use the
generator combinators that exist in the `org.scalacheck.Gen` module. These can
be combined using a for-comprehension. Suppose you need a generator which
generates a tuple that contains two random integer values, one of them being at
least twice as big as the other. The following definition does this:

```scala
val myGen = for {
  n <- Gen.choose(10,20)
  m <- Gen.choose(2*n, 500)
} yield (n,m)
```

You can create generators that picks one value out of a selection of values.
The following generator generates a vowel:

```scala
val vowel = Gen.oneOf('A', 'E', 'I', 'O', 'U', 'Y')
```

The `oneOf` method creates a generator that randomly picks one of its
parameters each time it generates a value. Notice that plain values are
implicitly converted to generators (which always generates that value) if
needed.

The distribution is uniform, but if you want to control it you can use the
`frequency` combinator:

```scala
val vowel = Gen.frequency(
  (3, 'A'),
  (4, 'E'),
  (2, 'I'),
  (3, 'O'),
  (1, 'U'),
  (1, 'Y')
)
```

Now, the `vowel` generator will generate E:s more often than Y:s. Roughly, 4/14
of the values generated will be E:s, and 1/14 of them will be Y:s.

#### Generating Case Classes

It is very simple to generate random instances of case classes in ScalaCheck.
Consider the following example where a binary integer tree is generated:

```scala
sealed abstract class Tree
case class Node(left: Tree, right: Tree, v: Int) extends Tree
case object Leaf extends Tree

import org.scalacheck._
import Gen._
import Arbitrary.arbitrary

val genLeaf = const(Leaf)

val genNode = for {
  v <- arbitrary[Int]
  left <- genTree
  right <- genTree
} yield Node(left, right, v)

def genTree: Gen[Tree] = oneOf(genLeaf, lzy(genNode))
```

We can now generate a sample tree:

```
scala> genTree.sample
res0: Option[Tree] = Some(Node(Leaf,Node(Node(Node(Node(Node(Node(Leaf,Leaf,-71),Node(Leaf,Leaf,-49),17),Leaf,-20),Leaf,-7),Node(Node(Leaf,Leaf,26),Leaf,-3),49),Leaf,84),-29))
```

#### Sized Generators

When ScalaCheck uses a generator to generate a value, it feeds it with some
parameters. One of the parameters the generator is given, is a *size* value,
which some generators use to generate their values. If you want to use the size
parameter in your own generator, you can use the `Gen.sized` method:

```scala
def matrix[T](g: Gen[T]): Gen[Seq[Seq[T]]] = Gen.sized { size =>
  val side = scala.math.sqrt(size).asInstanceOf[Int]
  Gen.listOfN(side, Gen.listOfN(side, g))
}
```

The `matrix` generator will use a given generator and create a matrix which
side is based on the generator size parameter. It uses the `Gen.listOfN` which
creates a sequence of given length filled with values obtained from the given
generator.

#### Conditional Generators

Conditional generators can be defined using `Gen.suchThat` in the following
way:

```scala
val smallEvenInteger = Gen.choose(0,200) suchThat (_ % 2 == 0)
```

Conditional generators work just like conditional properties, in the sense
that if the condition is too hard, ScalaCheck might not be able to generate
enough values, and it might report a property test as undecided. The
`smallEvenInteger` definition is probably OK, since it will only throw away
half of the generated numbers, but one has to be careful when using the
`suchThat` operator.

Note that if a property fails on a value generated through `suchThat`, and is
later shrunk (see [test case minimisation](#test-case-minimisation) below), the
value ultimately reported as failing might not satisfy the condition given to
`suchThat`. Although, it doesn't change the fact that there _exists_ a failing
case that does. To avoid confusion, the corresponding shrink for the type can
use `suchThat` method too.

#### Generating Buildables and Containers

There are special generators
* `Gen.buildableOf`, that generates any buildable collection
* `Gen.containerOf`, a convenience method for single parameter buildable like lists and arrays

They take another generator as argument, that is responsible for generating the individual items.
You can use it in the following way:

```scala
val genInt: Gen[Int]  = Gen.oneOf(1, 3, 5)
val genIntList        = Gen.containerOf[List, Int](genInt)
val genStringLazyList = Gen.containerOf[LazyList, String](Gen.alphaStr)
val genBoolArray      = Gen.containerOf[Array, Boolean](Gen.const(true))

val genTuple: Gen[(String, Int)] = Gen.zip(Gen.alphaStr, Gen.choose(0, 100))
val genMap                       = Gen.buildableOf[Map[String, Int], (String, Int)](genTuple)
```

By default, ScalaCheck supports generation of `List`, `Stream` (Scala 2.10 -
2.12, deprecated in 2.13), `LazyList` (Scala 2.13), `Set`, `Array` and `Map`.
Additionally, ScalaCheck can generate `java.util.ArrayList` and `java.util.HashMap` when an implicit 
`Traversable` conversion evidence is in scope.

You can add support for additional containers by adding implicit `Buildable` instances.
See `Buildable.scala` for examples.

There is also `Gen.nonEmptyContainerOf` for generating non-empty containers, and
`Gen.containerOfN` for generating containers of a given size.

To generate a container by picking an arbitrary number of elements use
`Gen.someOf`, or by picking one or more elements with `Gen.atLeastOne`.

```scala
val zeroOrMoreDigits = Gen.someOf(1 to 9)

val oneOrMoreDigits = Gen.atLeastOne(1 to 9)
```

Here are generators that randomly pick `n` elements from a container
with `Gen.pick`:

```scala
val fiveDice: Gen[Seq[Int]] = Gen.pick(5, 1 to 6)

val threeLetters: Gen[Seq[Char]] = Gen.pick(3, 'A' to 'Z')
```

Note that `Gen.someOf`, `Gen.atLeastOne`, and `Gen.pick` only randomly
select elements.  They do not generate permutations of the result
with elements in different orders.

To make your generator artificially permute the order of elements, you
can run `scala.util.Random.shuffle` on each of the generated containers
with the `map` method.

```scala
import scala.util.Random

val threeLettersPermuted = threeLetters.map(Random.shuffle(_))
```

#### The `arbitrary` Generator

There is a special generator, `org.scalacheck.Arbitrary.arbitrary`, which
generates arbitrary values of any supported type.

```scala
val evenInteger = Arbitrary.arbitrary[Int] suchThat (_ % 2 == 0)

val squares = for {
  xs <- Arbitrary.arbitrary[List[Int]]
} yield xs.map(x => x*x)
```

The `arbitrary` generator is the generator used by ScalaCheck when it generates
values for property parameters. Most of the times, you have to supply the type
of the value to `arbitrary`, like above, since Scala often can't infer the type
automatically. You can use `arbitrary` for any type that has an implicit
`Arbitrary` instance. As mentioned earlier, ScalaCheck has default support for
common types, but it is also possible to define your own implicit `Arbitrary`
instances for unsupported types. See the following implicit `Arbitrary`
definition for booleans, that comes from the ScalaCheck implementation.

```scala
implicit lazy val arbBool: Arbitrary[Boolean] = Arbitrary(oneOf(true, false))
```

To get support for your own type `T` you need to define an implicit `def` or
`val` of type `Arbitrary[T]`. Use the factory method `Arbitrary(...)` to create
the `Arbitrary` instance. This method takes one parameter of type `Gen[T]` and
returns an instance of `Arbitrary[T]`.

Now, let's say you have a custom type `Tree[T]` that you want to use as a
parameter in your properties:

```scala
abstract sealed class Tree[T] {
  def merge(t: Tree[T]) = Internal(List(this, t))

  def size: Int = this match {
    case Leaf(_) => 1
    case Internal(children) => (children :\ 0) (_.size + _)
  }
}

case class Internal[T](children: Seq[Tree[T]]) extends Tree[T]

case class Leaf[T](elem: T) extends Tree[T]
```

When you specify an implicit generator for your type `Tree[T]`, you also have
to assume that there exists an implicit generator for the type `T`. You do this
by specifying an implicit parameter of type `Arbitrary[T]`, so you can use the
generator `arbitrary[T]`.

```scala
implicit def arbTree[T](implicit a: Arbitrary[T]): Arbitrary[Tree[T]] =
Arbitrary {
  val genLeaf = for(e <- Arbitrary.arbitrary[T]) yield Leaf(e)

  def genInternal(sz: Int): Gen[Tree[T]] = for {
    n <- Gen.choose(sz/3, sz/2)
    c <- Gen.listOfN(n, sizedTree(sz/2))
  } yield Internal(c)

  def sizedTree(sz: Int) =
    if(sz <= 0) genLeaf
    else Gen.frequency((1, genLeaf), (3, genInternal(sz)))

  Gen.sized(sz => sizedTree(sz))
}
```

As long as the implicit `arbTree` function is in scope, you can now write
properties like this:

```scala
val propMergeTree = forAll( (t1: Tree[Int], t2: Tree[Int]) =>
  t1.size + t2.size == t1.merge(t2).size
```

#### Collecting Generated Test Data

It is possible to collect statistics about what kind of test data that has been
generated during property evaluation. This is useful if you want to inspect the
test case distribution, and make sure your property tests all different kinds
of cases, not just trivial ones.

For example, you might have a method that operates on lists, and which behaves
differently if the list is sorted or not. Then it is crucial to know if
ScalaCheck tests the method with both sorted and unsorted lists. Let us first
define an `ordered` method to help us state the property.

```scala
def ordered(l: List[Int]) = l == l.sorted
```

Now state the property, using `Prop.classify` to collect interesting
information on the generated data. The property itself is not very exciting in
this example, we just state that a double reverse should return the original
list.

```scala
import org.scalacheck.Prop._

val myProp = forAll { (l: List[Int]) =>
  classify(ordered(l), "ordered") {
    classify(l.length > 5, "large", "small") {
      l.reverse.reverse == l
    }
  }
}
```

Check the property, and watch the statistics printed by ScalaCheck:

```
scala> myProp.check
+ OK, passed 100 tests.
> Collected test data:
78% large
16% small, ordered
6% small
```

Here ScalaCheck tells us that the property hasn't been tested with any large
and ordered list (which is no surprise, since the lists are randomised).
Maybe we need to use a special generator that generates also large ordered
lists, if that is important for testing our method thoroughly. In this
particular case, it doesn't matter, since the implementation of `reverse`
probably doesn't care about whether the list is sorted or not.

We can also collect data directly, using the `Prop.collect` method. In this
dummy property, we just want to see if ScalaCheck distributes the generated
data evenly:

```
val dummyProp = forAll(Gen.choose(1,10)) { n =>
  collect(n) {
    n == n
  }
}

scala> dummyProp.check
+ OK, passed 100 tests.
> Collected test data:
13% 7
13% 5
12% 1
12% 6
11% 2
9% 9
9% 3
8% 10
7% 8
6% 4
```

As we can see, the frequency for each number is around 10%, which seems
reasonable.

### Test Execution

As we've seen, we can test properties or property collections by using the
`check` method. In fact, the `check` method is just a convenient way of running
`org.scalacheck.Test.check` (or `Test.checkProperties`, for property
collections).

The `Test` module is responsible for all test execution in ScalaCheck. It will
generate the arguments and evaluate the properties, repeatedly with larger and
larger test data (by increasing the _size_ parameter used by the generators).
If it doesn't manage to find a failing test case after a certain number of
tests, it reports a property as passed.

The `check` method looks like this:

```scala
def check(params: Test.Parameters, p: Prop): Test.Result
```

`Test.Parameters` is a trait that encapsulates testing parameters such as the
number of times a property should be tested, the size bounds of the test data,
and how many times ScalaCheck should try if it fails to generate arguments.
There are also field for callbacks in the `Parameters` record, if you need to
get feedback from the test runner programmatically.

The `check` method returns an instance of `Test.Result` which encapsulates
the result and some statistics of the property test. `Test.Result.status` is of
the type `Test.Status` and can have the following values:

```scala
/** ScalaCheck found enough cases for which the property holds, so the
 *  property is considered correct. (It is not proved correct, though). */
case object Passed extends Status

/** ScalaCheck managed to prove the property correct */
sealed case class Proved(args: List[Arg]) extends Status

/** The property was proved wrong with the given concrete arguments.  */
sealed case class Failed(args: List[Arg], label: String) extends Status

/** The property test was exhausted, it wasn't possible to generate enough
 *  concrete arguments satisfying the preconditions to get enough passing
 *  property evaluations. */
case object Exhausted extends Status

/** An exception was raised when trying to evaluate the property with the
 *  given concrete arguments. */
sealed case class PropException(args: List[Arg], e: Throwable, label: String) extends Status

/** An exception was raised when trying to generate concrete arguments
 *  for evaluating the property. */
sealed case class GenException(e: Throwable) extends Status
```

The `checkProperties` returns test statistics for each property in the tested
property collection, as a list. See the API documentation for more details.

It is also possible to test your properties from the command line. Each
property and property collection actually has a main method that can parse the
test parameters given to it. If you provide the argument `-h`, you will get a
list of possible arguments:

```
$ scala -cp scalacheck.jar MyProp -h
Incorrect options:
[1.1] failure: option name expected

-h
^

Available options:
  -workers, -w: Number of threads to execute in parallel for testing
  -minSize, -n: Minimum data generation size
  -verbosity, -v: Verbosity level
  -minSuccessfulTests, -s: Number of tests that must succeed in order to pass a property
  -maxDiscardRatio, -r: The maximum ratio between discarded and succeeded tests allowed before ScalaCheck stops testing a property. At least minSuccessfulTests will always be tested, though.
  -maxSize, -x: Maximum data generation size
  -propFilter, -f: Regular expression to filter properties on
```

These command line arguments can also be used in SBT to tweak ScalaCheck's
testing parameters when you run ScalaCheck tests through SBT. See SBT's
documentation for info on how to provide the arguments.

### Test Case Minimisation

One interesting feature of ScalaCheck is that if it finds an argument that
falsifies a property, it tries to _minimise_ that argument before it is
reported. This is done automatically when you use the `Prop.property` and
`Prop.forAll` methods to create properties, but not if you use
`Prop.forAllNoShrink`. Let's look at the difference between these methods, by
specifying a property that says that no list has duplicate elements in it. This
is of course not true, but we want to see the test case minimisation in action!

```scala
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.{forAll, forAllNoShrink}

val p1 = forAllNoShrink(arbitrary[List[Int]])(l => l == l.distinct)

val p2 = forAll(arbitrary[List[Int]])(l => l == l.distinct)

val p3 = forAll( (l: List[Int]) => l == l.distinct )
```

Now, run the tests:

```
scala> p1.check
! Falsified after 11 passed tests:
> ARG_0 = "List(8, 0, -1, -3, -8, 8, 2, -10, 9, 1, -8)"

scala> p2.check
! Falsified after 4 passed tests:
> ARG_0 = "List(-1, -1)" (2 shrinks)

scala> p3.check
! Falsified after 7 passed tests:
> ARG_0 = "List(-5, -5)" (3 shrinks)
```

In all cases, ScalaCheck found a list with duplicate elements that falsified
the property. However, in the two second cases the list was shrunk into a list
with just two identical elements in it, which is the minimal failing test case
for the given property. Clearly, it's much easier to find a bug if you are
given a simple test case that causes the failure.

Just as you can define implicit `Arbitrary` generators for your own types, you
can also define default shrinking methods. This is done by defining an implicit
method that returns a `Shrink[T]` instance. This is done by using the
`Shrink(...)` factory method, which as its only parameter takes a function and
returns an instance of `Shrink[T]`. In turn, the function should take a value
of the given type `T`, and return a `Stream` of shrank
variants of the given value. As an example, look at the implicit `Shrink` instance
for a tuple as it is defined in ScalaCheck:

```scala
/** Shrink instance of 2-tuple */
implicit def shrinkTuple2[T1,T2](implicit s1: Shrink[T1], s2: Shrink[T2]
): Shrink[(T1,T2)] = Shrink { case (t1,t2) =>
  (for(x1 <- shrink(t1)) yield (x1, t2)) append
  (for(x2 <- shrink(t2)) yield (t1, x2))
}
```

When implementing a shrinking method, one has to be careful to only return
*smaller* variants of the value, since the shrinking algorithm otherwise could
loop. ScalaCheck has implicit shrinking methods for common types such as integers
and lists.

If the generator for a type is restricting the range of valid values by
construction or using `Gen.suchThat`, the values that fail tests can still be
shrunk without checking the condition, and then ultimately be reported as
failing even though they do not satisfy it. To avoid that, use `Shrink.suchThat`
with the condition to be maintained:

```scala
/** Generate lists of an even length */
val genEvenList: Gen[List[Int]] = Gen.sized { size =>
  Gen.listOfN(size * 2, Arbitrary.arbitrary[Int])
}

/** Shrink a list, maintaining even length */
implicit val shrinkEvenList: Shrink[List[Int]] =
  Shrink.shrinkContainer[List,Int].suchThat(_.length % 2 == 0)
```

### Stateful Testing

We have described how ScalaCheck can be used to state properties about
isolated units of your code (usually methods), and how such
properties can be tested in an automated fashion. However, sometimes you want
to not only specify how a method should behave on its own, but also how a
collection of methods should behave together, when used as an interface to a
larger system. You want to specify how the methods - or *commands* - affect the
system's *state* throughout time.

An example could be to specify the workflow of an ATM. You'd want to state
requirements such as that the user has to enter the correct PIN code before any
money could be withdrawn, or that entering an erroneous PIN code three times
would make the machine confiscate the credit card.

Formalising such command sequences using ScalaCheck's property combinators is a
bit tricky. Instead, there is a small library in
`org.scalacheck.commands.Commands` for modelling commands and specifying
conditions about them, which can then be used just as ordinary ScalaCheck
properties, and tested with the `org.scalacheck.Test` module.

Let us now assume we want to test the following trivial counter class:

```scala
class Counter {
  private var n = 0
  def inc = n += 1
  def dec = n -= 1
  def get = n
  def reset = n = 0
}
```

`Counter` is the type of our system under test. ScalaCheck supports testing
systems in parallel (multiple instances of the system under test), or
sequentially (a singleton system). In both cases, the system under test is
represented by the abstract type `Commands.Sut`. In our example, the type `Sut`
will be set to `Counter`.

We will also need a type encoding the state of the system under test. This is
represented by the abstract type `Commands.State`. The `State` type should
describe enough of the actual system's state for us to be able to define
properties about the various commands. In our case, we will set `State = Int`,
and this actually models the real system state exactly. This is a coincidence
caused by the simplicity of our `Counter` implementation. A realistic system
would probably have other internal state that we wouldn't need (or even know
how) to implement in our abstract representation of the state.

Finally, the commands are modelled by implementing the `Commands.Command` type.
An example will make it easier to understand. We will model the commands `inc`,
`dec`, `get` and `reset`. The scaladoc comments are inlined in the example
below to help you out:

```scala
import org.scalacheck.commands.Commands
import org.scalacheck.Gen
import org.scalacheck.Prop
import scala.util.{Try, Success}

object CounterSpecification extends Commands {

  type State = Int

  type Sut = Counter

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
    runningSuts: Traversable[Sut]): Boolean = true

  /** The precondition for the initial state, when no commands yet have
   *  run. This is used by ScalaCheck when command sequences are shrinked
   *  and the first state might differ from what is returned from
   *  [[genInitialState]]. */
  def initialPreCondition(state: State): Boolean = {
    // Since the counter implementation doesn't allow initialisation with an
    // arbitrary number, we can only start from zero
    state == 0
  }

  /** Create a new [[Sut]] instance with an internal state that
   *  corresponds to the provided abstract state instance. The provided state
   *  is guaranteed to fulfill [[initialPreCondition]], and
   *  [[newSut]] will never be called if
   *  [[canCreateNewSut]] is not true for the given state. */
  def newSut(state: State): Sut = new Counter

  /** Destroy the system represented by the given [[Sut]]
   *  instance, and release any resources related to it. */
  def destroySut(sut: Sut): Unit = ()

  /** A generator that should produce an initial [[State]] instance that is
   *  usable by [[newSut]] to create a new system under test.
   *  The state returned by this generator is always checked with the
   *  [[initialPreCondition]] method before it is used. */
  def genInitialState: Gen[State] = Gen.const(0)

  /** A generator that, given the current abstract state, should produce
   *  a suitable Command instance. */
  def genCommand(state: State): Gen[Command] = Gen.oneOf(
    Inc, Get, Dec, Reset
  )

  // A UnitCommand is a command that doesn't produce a result
  case object Inc extends UnitCommand {
    def run(sut: Sut): Unit = sut.inc

    def nextState(state: State): State = state + 1

    // This command has no preconditions
    def preCondition(state: State): Boolean = true

    // This command should always succeed (never throw an exception)
    def postCondition(state: State, success: Boolean): Prop = success
  }

  case object Dec extends UnitCommand {
    def run(sut: Sut): Unit = sut.dec
    def nextState(state: State): State = state - 1
    def preCondition(state: State): Boolean = true
    def postCondition(state: State, success: Boolean): Prop = success
  }

  case object Reset extends UnitCommand {
    def run(sut: Sut): Unit = sut.reset
    def nextState(state: State): State = 0
    def preCondition(state: State): Boolean = true
    def postCondition(state: State, success: Boolean): Prop = success
  }

  case object Get extends Command {

    // The Get command returns an Int
    type Result = Int

    def run(sut: Sut): Result = sut.get

    def nextState(state: State): State = state

    def preCondition(state: State): Boolean = true

    // The post condition verifies that the result we get back from the
    // actual system corresponds to our model of the state
    def postCondition(state: State, result: Try[Result]): Prop = {
      result == Success(state)
    }
  }
}
```

Now we can test our `Counter` implementation. The `Commands` trait has a method
called `property` that returns a `Prop` instance that allows us to treat our
stateful system specification as an ordinary ScalaCheck property.

```
scala> CounterSpecification.property().check
+ OK, passed 100 tests.
```

OK, our implementation seems to work. But let us introduce a bug:

```scala
class Counter {
  private var n = 0
  def inc = n += 1
  def dec = if(n > 3) n -= 2 else n -= 1  // Bug!
  def get = n
  def reset = n = 0
}
```

Let's test it again:

```
scala> CounterSpecification.property().check
! Falsified after 64 passed tests.
> Labels of failing property:
initialstate = 0
seqcmds = (Inc; Inc; Inc; Inc; Dec; Get => 2)
> ARG_0: Actions(0,List(Inc, Inc, Inc, Inc, Dec, Get),List())
> ARG_0_ORIGINAL: Actions(0,List(Inc, Dec, Inc, Get, Inc, Inc, Inc, Dec, In
  c, Get, Dec, Dec, Inc, Dec, Dec, Get, Get, Inc, Dec, Reset, Reset, Dec, D
  ec, Dec, Get, Inc, Reset, Dec, Get, Dec, Dec, Get, Dec, Get, Inc, Reset,
  Dec, Inc, Reset, Get, Get, Reset, Inc, Reset, Dec, Inc, Dec, Dec, Get, In
  c, Inc, Reset, Get, Get, Inc, Dec, Dec, Dec, Reset, Get, Inc, Dec, Dec, I
  nc),List())
```

ScalaCheck found a failing command sequence (after testing 64 good ones), and
then shrank it down. The resulting command sequence is indeed the minimal
failing one! There is no other less complex command sequence that could have
discovered the bug. This is a very powerful feature when testing complicated
command sequences, where bugs may occur after a very specific sequence of
commands that is hard to come up with when doing manual tests.

You can find more examples of stateful specifications in the
[examples](https://github.com/typelevel/scalacheck/tree/main/examples)
directory in the ScalaCheck repository. The
[slides](http://scalacheck.org/files/scaladays2014/index.html) from the
ScalaDays 2014 presentation about stateful testing in ScalaCheck are also
helpful.
