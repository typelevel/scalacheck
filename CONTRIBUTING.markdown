# Developing ScalaCheck

ScalaCheck is developed using [sbt](http://www.scala-sbt.org).

You can start sbt with:

    $ sbt

You can run all the tests:

    > test

You can run a single test for Scala on Java (not Scala.js or native):

    > jvm/testOnly org.scalacheck.GenSpecification

More information on running tests from sbt are in the User Guide.

More information on the benchmark is in the README under `bench`.

If you want to interact with ScalaCheck in the console, you need to
use the `jvm` project:

    > jvm/console

The releases are done with the `release.sh` script.  If you change the
script, you can test it still works, but not publish artifacts, by
running a non-publishing command like `package`:

    $ ./release.sh package
