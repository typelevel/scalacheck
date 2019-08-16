name := "scalacheck-demo"

scalaVersion := "2.12.9"

val scalaCheckVersion = Option(System.getenv().get("TRAVIS_COMMIT"))
  .map("1.14.1-" + _.take(7) + "-SNAPSHOT")
  .getOrElse("1.14.0")

libraryDependencies += "org.scalacheck" %% "scalacheck" % scalaCheckVersion % "test"

testOptions in Test += Tests.Argument(TestFrameworks.ScalaCheck, "-maxSize", "5", "-minSuccessfulTests", "33", "-workers", "1", "-verbosity", "1")
