enablePlugins(ScalaJSPlugin)

name := "ScalaCheck-scalajs-example"

version := "0.1-SNAPSHOT"

scalaVersion := "2.12.4"

javacOptions += "-Xmx2048M"

libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.13.5" % "test"
