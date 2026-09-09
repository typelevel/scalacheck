name := "scalacheck-demo"

scalaVersion := "3.1.0"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.20.0" % Test

Test / testOptions += Tests.Argument(
  TestFrameworks.ScalaCheck,
  "-maxSize",
  "5",
  "-minSuccessfulTests",
  "33",
  "-workers",
  "1",
  "-verbosity",
  "1")
