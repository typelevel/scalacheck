enablePlugins(ScalaJSPlugin)

name := "ScalaCheck-scalajs-example"

version := "0.1-SNAPSHOT"

scalaVersion := "2.12.9"

javacOptions += "-Xmx2048M"

val scalaCheckVersion = Option(System.getenv().get("TRAVIS_COMMIT"))
  .map("1.14.1-" + _.take(7) + "-SNAPSHOT")
  .getOrElse("1.14.0")

libraryDependencies += "org.scalacheck" %%% "scalacheck" % scalaCheckVersion % "test"
