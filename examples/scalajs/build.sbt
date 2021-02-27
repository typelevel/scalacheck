enablePlugins(ScalaJSPlugin)

name := "ScalaCheck-scalajs-example"

version := "0.1-SNAPSHOT"

scalaVersion := "2.13.5"

libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.14.3" % "test"
