enablePlugins(ScalaJSPlugin)

name := "ScalaCheck-scalajs-example"

version := "0.1-SNAPSHOT"

scalaVersion := "3.0.2"

libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.15.4" % Test
