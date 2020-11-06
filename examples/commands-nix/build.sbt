name := "commands-nix"

scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.15.1" % "test",
  "net.java.dev.jna" % "jna" % "4.5.1"
)
