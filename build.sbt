name := "scalacheck"

version := "1.10-SNAPSHOT"

organization := "org.scala-tools.testing"

scalaVersion := "2.9.0-1"

libraryDependencies += "org.scala-tools.testing" %  "test-interface" % "0.5"

javacOptions ++= Seq("-Xmx1024M")

scalacOptions += "-deprecation"

// publishTo := Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/snapshots/")

publishTo := Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
