ScalaCheck is a library written in the Scala Programming Language and is used for automated specification-based testing of Scala or Java software applications. ScalaCheck was originally inspired by the Haskell library [QuickCheck](http://hackage.haskell.org/package/QuickCheck), but has also ventured into its own. 

* [The source code and issue handling for ScalaCheck is hosted on GitHub](http://github.com/rickynils/scalacheck)

* [Documentation and downloads are on Google Code](http://code.google.com/p/scalacheck/)

* [Snapshots and Releases published to the repository on scala-tools.org](http://scala-tools.org/repo-releases/org/scalacheck/scalacheck/)

* [The mailing list is hosted by Google Groups](http://groups.google.com/group/scalacheck)

Build Instructions
------------------

The root directory of the project contains the SBT launcher, shell script, and Windows command script.

This is the directory structure of the build.

           |-project  +
           |          |-build +
           |          |       |- ScalaCheckProject.scala  Project Definition, containing module structure, compiler
           |          |       |                           options, cross module dependencies, etc.
           |          |       |- build.properties         Version of SBT, Scala, and ScalaCheck.
           |          |                                   A different version of Scala is used to run SBT and compile
           |          |                                   the Project Definition than is used to compile ScalaCheck.
           |          |-target                            Compiled Project Definition
           |          |
           |          |-boot                              Versions of Scala Compiler and Library.
           |
           |-src   +
           |       |-main +
           |       |      |-scala                         Source files
           |       |
           |       |-test +
           |              |-scala                         Test source files
           |
           |-lib_managed                                  Managed Dependencies for this module.
           |
           |-target +
                    | - <scala version M>                 All built artifacts (classes, jars, scaladoc) for module N
                                                          built for version M of Scala.

1. ./sbt update (this step is required after a fresh checkout, after changing the version of
                     SBT, Scala, or other dependencies)
2. ./sbt [compile | package | test-compile | test | publish-local | publish]

For continuous compilation of a module:

    $ ./sbt
    > project scalacheck
    > ~compile

For other options, read [the SBT documentation](http://code.google.com/p/simple-build-tool/wiki/DocumentationHome).
