# ScalaCheck

ScalaCheck is a library written in the Scala Programming Language and is used
for automated specification-based testing of Scala or Java software
applications. ScalaCheck was originally inspired by the Haskell library
[QuickCheck](http://hackage.haskell.org/package/QuickCheck), but has also
ventured into its own.

ScalaCheck has no dependencies other than the Scala runtime, and is supported
by [SBT](https://github.com/harrah/xsbt/wiki),
[ScalaTest](http://www.scalatest.org/) and
[Specs2](http://etorreborre.github.com/specs2/). You can of course also use
ScalaCheck completely standalone, with its built-in test runner.

## Quick start

Specify some of the methods of `java.lang.String` like this:

    import org.scalacheck.Properties
    import org.scalacheck.Prop.forAll

    object StringSpecification extends Properties("String") {
      property("startsWith") = forAll((a: String, b: String) => (a+b).startsWith(a))

      property("concatenate") = forAll((a: String, b: String) =>
        (a+b).length > a.length && (a+b).length > b.length
      )

      property("substring") = forAll((a: String, b: String, c: String) =>
        (a+b+c).substring(a.length, a.length+b.length) == b
      )
    }

Then compile and run the tests like this:

    $ scalac -cp scalacheck.jar StringSpecification.scala

    $ scala -cp .:scalacheck.jar StringSpecification
    + String.startsWith: OK, passed 100 tests.
    ! String.concat: Falsified after 0 passed tests.
    > ARG_0: ""
    > ARG_1: ""
    + String.substring: OK, passed 100 tests.

As we can see, the second property was not quite right. ScalaCheck discovers
this and presents the arguments that make the property fail, two empty strings.
The other two properties both pass 100 test rounds, each with a randomized set
of input parameters.

## Documentation

* [ScalaDocs](http://rickynils.github.com/scalacheck)
* [User guide](https://github.com/rickynils/scalacheck/wiki/User-Guide)
* [Mailing list](http://groups.google.com/group/scalacheck)

## Download

The current release of ScalaCheck is 1.10.0, and it is available in the Sonatype
OSS repository. There should be builds available for all 2.9.x versions of
Scala. If you lack some build, please submit an issue. ScalaCheck 1.10.0 is
unfortunately not compatible with Scala 2.8.x.

The builds are available for download at
[SonaType](https://oss.sonatype.org/index.html#nexus-search;quick~scalacheck).
There you can also find source and scaladoc packages.

If you need to use ScalaCheck with an old version of Scala, please check out
the [previous releases](http://code.google.com/p/scalacheck/downloads/list).

Notice, ScalaCheck was previously published under the repository group id (or
organization) `org.scala-tools.testing`, but this has been changed to
`org.scalacheck`. Please make sure to update your dependencies. The current
ScalaCheck release might still be available under the old id, but new builds
will only be published under the new id.

If you are using SBT, add the following to your build file to make ScalaCheck
available in your project:

    resolvers ++= Seq(
      "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
      "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases"
    )

    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.10.0" % "test"
    )

If you are using Maven, the following will do the trick (example for Scala 2.9.2):

    <repositories>
      <repository>
        <id>oss.sonatype.org</id>
        <name>releases</name>
        <url>http://oss.sonatype.org/content/repositories/releases</url>
      </repository>
      <repository>
        <id>oss.sonatype.org</id>
        <name>snapshots</name>
        <url>http://oss.sonatype.org/content/repositories/snapshots</url>
      </repository>
    </repositories>

    <dependency>
      <groupId>org.scalacheck</groupId>
      <artifactId>scalacheck_2.9.2</artifactId>
      <version>1.10.0</version>
    </dependency>

## Bugs and feature requests

Please feel free to submit any bugs you find or feature requests you have in
the [issue tracker](https://github.com/rickynils/scalacheck/issues) here at
GitHub. Pull requests are of course also welcome!

## Build instructions

ScalaCheck uses SBT for building, and the root directory contains an SBT
launcher which makes building very easy. Just clone the git repository and
build ScalaCheck in the following way:

    ./sbt update
    ./sbt compile

Run the test suite like this:

    ./sbt test

The tests for ScalaCheck is of course written in ScalaCheck. SBT even handles
bootstrapping, so the newly built ScalaCheck will be used to run the tests on
itself.
