name := "scalacheck-demo"

scalaVersion := "2.11.2"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.0-SNAPSHOT" % "test"

testOptions in Test += Tests.Argument(TestFrameworks.ScalaCheck, "-maxSize", "5", "-minSuccessfulTests", "33", "-workers", "1", "-verbosity", "1")
