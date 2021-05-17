enablePlugins(ScalaJSPlugin)

name := "ScalaCheck-scalajs-example"

version := "0.1-SNAPSHOT"

scalaVersion := "2.13.6"

libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.15.3" % Test
