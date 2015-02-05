enablePlugins(ScalaJSPlugin)

name := "ScalaCheck-scalajs-example"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.5"

javacOptions += "-Xmx2048M"

libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.12.2" % "test"
