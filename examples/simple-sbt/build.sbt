name := "scalacheck-demo"

scalaVersion := "2.12.4"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.5" % "test"

testOptions in Test += Tests.Argument(TestFrameworks.ScalaCheck, "-maxSize", "5", "-minSuccessfulTests", "33", "-workers", "1", "-verbosity", "1")
