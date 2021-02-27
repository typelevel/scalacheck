name := "scalacheck-demo"

scalaVersion := "2.13.5"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.15.1" % "test"

Test / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-maxSize", "5", "-minSuccessfulTests", "33", "-workers", "1", "-verbosity", "1")
