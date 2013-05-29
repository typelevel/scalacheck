# A simple ScalaCheck sbt project

This is a minimal project with a single test that is meant to be used to
quickly get started with ScalaCheck.

## Setting up required software

The project can be built with [sbt](http://www.scala-sbt.org/). Follow the
instructions on
[http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html] to get
started. sbt is easy to set up on Windows, Mac and Linux, and its only
requirement is a working Java runtime installation of version 1.6 or later.

To install the latest Java runtime environment on your computer, follow the
instructions on [http://java.com/en/download/manual.jsp]. There are installers
available for all major operating systems.

## Setting up the ScalaCheck example project

When you have installed sbt and made it available in your path, you should
replicate this example project on your computer (the file `build.sbt`, and the
complete `src` directory structure). You can do it by manually setting up the
directory structure and file contents, or by downloading the complete
ScalaCheck repository.

To then test the ScalaCheck properties with sbt, run the command `sbt test` in
the root directory of the project (the directory where the file `build.sbt` is
located):

    examples/simple-sbt $ sbt test
    Getting net.java.dev.jna jna 3.2.3 ...
    Getting org.scala-sbt sbt 0.12.3 ...
    ...
    [info] Done updating.
    [info] Compiling 1 Scala source to .../scala-2.10/test-classes...
    [info] + Demo.myprop: OK, passed 100 tests.
    [info] Passed: : Total 1, Failed 0, Errors 0, Passed 1, Skipped 0
    [success] Total time: 10 s, completed May 23, 2013 8:09:01 AM

sbt will automatically download all needed dependencies. The output from the
above command might look differently if you already have some or all
dependencies installed or cached.

The last three lines of the output is what you should look for in your own
output. It tells us that the property `Demo.myprop` passed 100 evaluations, and
then gives a summary of the results (in this case, there's only one property,
so the summary is not that interesting).

Now you can go ahead and experiment by modifying the existing property or
adding new ones. sbt will pick up any Scala file that contains ScalaCheck
properties, if it is located somewhere under `src/test/scala`. Look at the
existing file `src/test/scala/Demo.scala` for inspiration.
