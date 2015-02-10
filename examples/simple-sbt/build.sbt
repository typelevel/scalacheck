name := "scalacheck-demo"

scalaVersion := "2.11.5"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.2" % "test"

testOptions in Test += Tests.Argument(TestFrameworks.ScalaCheck, "-maxSize", "5", "-minSuccessfulTests", "33", "-workers", "1", "-verbosity", "1")
