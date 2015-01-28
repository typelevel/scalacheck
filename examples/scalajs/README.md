# Using ScalaCheck with Scala.js

ScalaCheck has experimental support for [Scala.js](http://www.scala-js.org/).
The current master branch of ScalaCheck has been ported to work with Scala.js,
and a snapshot version has been released. Not all parts of ScalaCheck works
yet, but around 40% of ScalaCheck's own test suite runs fine under Scala.js.
Feel free reporting issues when you find things that don't work as expected.

## Get started

To get started, open `sbt` in this example project, and execute the task
`test`. This should execute the ScalaCheck properties found in the `src/test` directory.

You will find that test execution is very slow, but you can speed it up
considerably by installing [Node.js](http://nodejs.org/), and setting the sbt setting `scalaJSStage` to `FastOptStage`.
For example, for a project `js` this can be set on the command line with:

```
set scalaJSStage in js := FastOptStage
```

The following is what you need to add to your `build.sbt` file to make the
ScalaCheck test runner work:

```
resolvers += "sonatype-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.12.2-SNAPSHOT" % "test"

ScalaJSKeys.scalaJSTestFramework := "org.scalacheck.ScalaCheckFramework"
```

## Limitations

The ScalaCheck Scala.js test runner doesn't parse any options yet, so you can't
change parameters like `minSuccessfulTests` yet. That parameter defaults to
`10` for Scala.js tests, not the usual `100`.

No attempts have yet been made on making ScalaCheck work together with other
test frameworks for Scala.js.
