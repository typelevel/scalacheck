name := "scalacheck-demo"

scalaVersion := "2.13.6"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.15.3" % Test

Test / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-maxSize", "5", "-minSuccessfulTests", "33", "-workers", "1", "-verbosity", "1")
