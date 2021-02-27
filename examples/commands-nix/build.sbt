name := "commands-nix"

scalaVersion := "2.13.5"

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.15.3" % Test,
  "net.java.dev.jna" % "jna" % "4.5.1"
)
