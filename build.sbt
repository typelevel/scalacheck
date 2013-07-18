import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact

name := "scalacheck"

version := "1.10.2-SNAPSHOT"

organization := "org.scalacheck"

licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php"))

homepage := Some(url("http://www.scalacheck.org"))

scalaVersion := "2.10.1"

crossScalaVersions := Seq("2.9.0", "2.9.0-1", "2.9.1", "2.9.1-1", "2.9.2", "2.9.3", "2.10.0", "2.10.1")

mimaDefaultSettings

previousArtifact := Some("org.scalacheck" % "scalacheck_2.10" % "1.10.1")

libraryDependencies += "org.scala-tools.testing" %  "test-interface" % "0.5"


libraryDependencies <++= (scalaVersion){sVer =>
  if(sVer startsWith "2.9") Seq.empty
  else Seq("org.scala-lang" % "scala-actors" % sVer)
}


libraryDependencies <++= (scalaVersion){sVer =>
  if((sVer startsWith "2.9") || (sVer startsWith "2.10")) Seq.empty
  else Seq("org.scala-lang" % "scala-parser-combinators" % sVer)
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
