import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact

name := "scalacheck"

version := "1.10.0-b1"

organization := "org.scalacheck"

licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php"))

homepage := Some(url("http://www.scalacheck.org"))

scalaVersion := "2.9.2"

crossScalaVersions := Seq("2.9.0", "2.9.0-1", "2.9.1", "2.9.1-1", "2.9.2", "2.10.0-M1", "2.10.0-M2", "2.10.0-M3", "2.10.0-M4")

mimaDefaultSettings

previousArtifact := Some("org.scalacheck" % "scalacheck_2.9.2" % "1.9")

libraryDependencies += "org.scala-tools.testing" %  "test-interface" % "0.5"

libraryDependencies <++= (scalaVersion){sVer =>
  sVer match {
    case "2.10.0-M3" => Seq("org.scala-lang" % "scala-actors" % sVer)
    case "2.10.0-M4" => Seq("org.scala-lang" % "scala-actors" % sVer)
    case _ => Seq()
  }
}

javacOptions ++= Seq("-Xmx1024M")

scalacOptions += "-deprecation"

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <scm>
    <url>git@github.com:rickynils/scalacheck.git</url>
    <connection>scm:git:git@github.com:rickynils/scalacheck.git</connection>
  </scm>
  <developers>
    <developer>
      <id>rickynils</id>
      <name>Rickard Nilsson</name>
      <url>http://www.scalacheck.org</url>
    </developer>
  </developers>
)
