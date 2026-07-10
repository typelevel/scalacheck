# Introduction

## ScalaCheck: Property-based testing for Scala

ScalaCheck is a library written in [Scala](https://www.scala-lang.org/) and
used for automated property-based testing of Scala or Java programs.
ScalaCheck was originally inspired by the Haskell library
[QuickCheck](https://hackage.haskell.org/package/QuickCheck), but has also
ventured into its own.

ScalaCheck has no external dependencies other than the Scala runtime, and
[works](download.md) great with [sbt](https://www.scala-sbt.org/), the
Scala build tool. It is also fully integrated in the test frameworks
[ScalaTest](https://www.scalatest.org/),
[specs2](https://etorreborre.github.io/specs2/) and
[LambdaTest](https://github.com/47deg/LambdaTest). You can also use ScalaCheck
completely standalone, with its built-in test runner.

ScalaCheck is used by several prominent Scala projects, for example, the [Scala
compiler](https://www.scala-lang.org/) and the [Akka](https://akka.io/)
concurrency framework.

[ScalaCheck 1.19.0]: https://github.com/typelevel/scalacheck/releases/tag/v1.19.0
[ScalaCheck 1.18.1]: https://github.com/typelevel/scalacheck/releases/tag/v1.18.1
[ScalaCheck 1.18.0]: https://github.com/typelevel/scalacheck/releases/tag/v1.18.0
[ScalaCheck 1.17.1]: https://github.com/typelevel/scalacheck/releases/tag/v1.17.1
[ScalaCheck 1.17.0]: https://github.com/typelevel/scalacheck/releases/tag/v1.17.0
[ScalaCheck 1.16.0]: https://github.com/typelevel/scalacheck/releases/tag/v1.16.0
[ScalaCheck 1.15.4]: https://github.com/typelevel/scalacheck/releases/tag/1.15.4
[ScalaCheck 1.15.3]: https://github.com/typelevel/scalacheck/releases/tag/1.15.3
[ScalaCheck 1.15.2]: https://github.com/typelevel/scalacheck/releases/tag/1.15.2
[ScalaCheck 1.15.1]: https://github.com/typelevel/scalacheck/releases/tag/1.15.1
[ScalaCheck 1.15.0]: https://github.com/typelevel/scalacheck/releases/tag/1.15.0
[ScalaCheck 1.14.3]: https://github.com/typelevel/scalacheck/releases/tag/1.14.3
[ScalaCheck 1.14.2]: https://github.com/typelevel/scalacheck/releases/tag/1.14.2
[ScalaCheck 1.14.1]: https://github.com/typelevel/scalacheck/releases/tag/1.14.1
[ScalaCheck 1.14.0]: https://github.com/typelevel/scalacheck/releases/tag/1.14.0
[ScalaCheck 1.13.5]: https://github.com/typelevel/scalacheck/releases/tag/1.13.5
[ScalaCheck 1.13.4]: https://github.com/typelevel/scalacheck/releases/tag/1.13.4
[ScalaCheck 1.13.3]: https://github.com/typelevel/scalacheck/releases/tag/1.13.3
[ScalaCheck 1.13.2]: https://github.com/typelevel/scalacheck/releases/tag/1.13.2
[ScalaCheck 1.13.1]: https://github.com/typelevel/scalacheck/releases/tag/1.13.1
[ScalaCheck 1.13.0]: https://github.com/typelevel/scalacheck/releases/tag/1.13.0
[ScalaCheck 1.12.6]: https://github.com/typelevel/scalacheck/releases/tag/1.12.16

## News

* 2025-09-06: [ScalaCheck 1.19.0] is released.
  New `Gen.zipWith` combinator. Bug fixes for `Gen.pick` and `ScalaCheckFramework`.

* 2024-09-15: [ScalaCheck 1.18.1] is released.
  Maintenance release.

* 2024-04-17: [ScalaCheck 1.18.0] is released.
  Support for Scala Native v0.5.

* 2024-04-16: [ScalaCheck 1.17.1] is released.
  Several bug fixes. New `Gen.nonEmptyStringOf` combinator.

* 2022-09-15: [ScalaCheck 1.17.0] is released.
  Several bug fixes and improvements.

* 2022-04-07: [ScalaCheck 1.16.0] is released.
  Support for `java.time` types. Several bug fixes.

* 2021-05-14: Scala 3.0 artifacts were published for [ScalaCheck 1.15.4]
  and [ScalaCheck 1.15.3]

* 2021-05-03: [ScalaCheck 1.15.4] is released with a few small bug fixes

* 2021-02-16: [ScalaCheck 1.15.3] adds support for Scala Native 0.4.0

* 2020-12-17: [ScalaCheck 1.15.2] fixes some uninentional API breakage of
  methods `someOf`, `atLeastOne` and `pick` in `Gen`.

* 2020-11-03: [ScalaCheck 1.15.1]
  is released. Fixes some uninentional API breakage of methods `someOf`,
  `atLeastOne` and `pick` in `Gen`.

* 2020-10-30: [ScalaCheck 1.15.0]
  introduces a few enhancements and some performance improvements to string
  and character generation.

* 2019-12-13: [ScalaCheck 1.14.3] fixed a defect with number generators. This
  also produces artifacts for Scala.js 1.0.0-RC2 and 0.6.31.

* 2019-09-25: [ScalaCheck 1.14.2] fixed a major defect with Scala.js but no
  other changes from 1.14.1.

* 2019-09-18: [ScalaCheck 1.14.1] released! This is the first release since the
  ScalaCheck repository was moved to the [Typelevel](https://typelevel.org/)
  organisation.

* 2018-04-22: [ScalaCheck 1.14.0]! This release
  introduces support for deterministic testing. See the release and change notes
  for more details.

* 2016-11-01: [ScalaCheck 1.13.4] and
  [ScalaCheck 1.12.6] released! These releases
  fix a [deadlock problem](https://github.com/rickynils/scalacheck/issues/290)
  with Scala 2.12.0. Also, as a problem was discovered with the
  previously released ScalaCheck 1.12.5 build, it is recommended that you
  upgrade to 1.12.6 or 1.13.4 even if you're not using Scala 2.12.

* 2016-10-19: [ScalaCheck 1.13.3] released! See the release and change notes
  for more details.

* 2016-07-11: [ScalaCheck 1.13.2] released! See the release and change notes
  for more details.

* 2016-04-14: [ScalaCheck 1.13.1] released! See the release and change notes
  for more details.

* 2016-02-03: [ScalaCheck 1.13.0] released! See the release and change notes
  for more details.

* 2015-09-10: Snapshot builds are no longer published on this site.
  Instead, [Travis](https://travis-ci.org/rickynils/scalacheck)
  automatically publishes all successful builds of the `main` branch.

## Quick start

Specify some methods of `java.lang.String` like this:

```scala
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll

object StringSpecification extends Properties("String") {

  property("startsWith") = forAll { (a: String, b: String) =>
    (a+b).startsWith(a)
  }

  property("concatenate") = forAll { (a: String, b: String) =>
    (a+b).length > a.length && (a+b).length > b.length
  }

  property("substring") = forAll { (a: String, b: String, c: String) =>
    (a+b+c).substring(a.length, a.length+b.length) == b
  }

}
```

If you use [sbt](https://www.scala-sbt.org/) add the following dependency to
your build file:

```scala
libraryDependencies += "org.scalacheck" %% "scalacheck" % "$currentVer$" % "test"
```

Put your ScalaCheck properties in `src/test/scala`, then use the `test` task to
check them:

```
$$ sbt test
+ String.startsWith: OK, passed 100 tests.
! String.concat: Falsified after 0 passed tests.
> ARG_0: ""
> ARG_1: ""
+ String.substring: OK, passed 100 tests.
```

As you can see, the second property was not quite right. ScalaCheck discovers
this and presents the arguments that make the property fail, two empty strings.
The other two properties both pass 100 test rounds, each with a randomized set
of input parameters.
