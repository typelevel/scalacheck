name := "commands-nix"

scalaVersion := "3.1.3"

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.15.4" % Test,
  "net.java.dev.jna" % "jna" % "4.5.1"
)
