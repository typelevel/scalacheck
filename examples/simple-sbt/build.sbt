name := "scalacheck-demo"

scalaVersion := "3.0.2"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.15.4" % Test

Test / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-maxSize", "5", "-minSuccessfulTests", "33", "-workers", "1", "-verbosity", "1")
