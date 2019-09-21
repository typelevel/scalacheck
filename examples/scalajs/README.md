# Using ScalaCheck with Scala.js

ScalaCheck has experimental support for [Scala.js](http://www.scala-js.org/).
The current release of ScalaCheck has artifacts published to work with Scala.js.
Not all parts of ScalaCheck work in Scala.js,
but around 40% of ScalaCheck's own test suite runs fine under Scala.js.
Feel free reporting issues when you find things that don't work as expected.

## Get started

To get started, open `sbt` in this example project, and execute the task
`test`. This should execute the ScalaCheck properties found in the `src/test` directory.

You will find that test execution is very slow, but you can speed it up
considerably by installing [Node.js](http://nodejs.org/), and setting the sbt
setting `scalaJSStage` to `FastOptStage`.  For example, for a project `js` this
can be set on the command line with:

```
set scalaJSStage in js := FastOptStage
```

The following is what you need to add to your `build.sbt` file to make the
ScalaCheck test runner work:

```
libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.14.1" % "test"
```

## Limitations

No attempts have yet been made on making ScalaCheck work together with other
test frameworks for Scala.js.
