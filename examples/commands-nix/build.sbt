name := "commands-nix"

scalaVersion := "3.3.8"

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.20.0" % Test,
  "net.java.dev.jna" % "jna" % "5.19.1"
)
