# ScalaCheck

[![Join the chat at https://gitter.im/scalacheck/Lobby](https://badges.gitter.im/scalacheck/Lobby.svg)](https://gitter.im/scalacheck/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build Status](https://travis-ci.org/typelevel/scalacheck.svg?branch=master)](http://travis-ci.org/typelevel/scalacheck)

ScalaCheck is a library written in [Scala](http://www.scala-lang.org/) and
used for automated property-based testing of Scala or Java programs.
ScalaCheck was originally inspired by the Haskell library
[QuickCheck](http://hackage.haskell.org/package/QuickCheck), but has also
ventured into its own.

ScalaCheck has no external dependencies other than the Scala runtime, and
[works](http://www.scalacheck.org/download.html#sbt) great with [SBT](http://www.scala-sbt.org/), the
Scala build tool. It is also fully integrated in the test frameworks
[ScalaTest](http://www.scalatest.org/) and
[specs2](http://etorreborre.github.com/specs2/). You can of course also use
ScalaCheck completely standalone, with its built-in test runner.

ScalaCheck is used by several prominent Scala projects, for example the [Scala
compiler](http://www.scala-lang.org/) and the [Akka](http://akka.io/)
concurrency framework.

**For more information and downloads, please visit http://www.scalacheck.org**

## Developing ScalaCheck

ScalaCheck is developed using [SBT](http://www.scala-sbt.org).

### Benchmarks

To run ScalaCheck's benchmarks, run the following command from SBT:

```
bench/jmh:run -wi 5 -i 5 -f1 -t1 org.scalacheck.bench.GenBench.*
```

The required parameters are:

 * `-wi` the number of warmup intervals to run
 * `-i` the number of benchmarking intervals to run
 * `-f` the number of forked processes to use during benchmarking
 * `-t` the number of threads to use during benchmarking

Smaller numbers will run faster but with less accuracy.

For more information about how we benchmark ScalaCheck, please see the
comments in `GenBench.scala`.
