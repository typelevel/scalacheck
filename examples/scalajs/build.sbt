enablePlugins(ScalaJSPlugin)

name := "ScalaCheck-scalajs-example"

version := "0.1-SNAPSHOT"

scalaVersion := "3.1.0"

libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.15.4" % Test
