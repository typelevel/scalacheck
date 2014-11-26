scalaJSSettings

name := "ScalaCheck-scalajs-example"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.4"

javacOptions += "-Xmx2048M"

resolvers += "sonatype-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.12.1-SNAPSHOT" % "test"

ScalaJSKeys.scalaJSTestFramework := "org.scalacheck.ScalaCheckFramework"
