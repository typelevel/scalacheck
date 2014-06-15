name := "commands-nix"

scalaVersion := "2.11.1"

resolvers += 
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.12.0-SNAPSHOT",
  "net.java.dev.jna" % "jna" % "4.0.0"
)

javacOptions ++= Seq("-Xmx1024M")
