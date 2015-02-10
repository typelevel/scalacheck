name := "commands-nix"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.12.2",
  "net.java.dev.jna" % "jna" % "4.0.0"
)

javacOptions ++= Seq("-Xmx1024M")
