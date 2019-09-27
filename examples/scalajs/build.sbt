enablePlugins(ScalaJSPlugin)

name := "ScalaCheck-scalajs-example"

version := "0.1-SNAPSHOT"

scalaVersion := "2.12.10"

libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.14.2" % "test"
