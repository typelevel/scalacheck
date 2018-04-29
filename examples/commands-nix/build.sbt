name := "commands-nix"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.14.0",
  "net.java.dev.jna" % "jna" % "4.0.0"
)

javacOptions ++= Seq("-Xmx1024M")
