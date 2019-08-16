name := "commands-nix"

scalaVersion := "2.12.9"

val scalaCheckVersion = Option(System.getenv().get("TRAVIS_COMMIT"))
  .map("1.14.1-" + _.take(7) + "-SNAPSHOT")
  .getOrElse("1.14.0")

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % scalaCheckVersion % "test",
  "net.java.dev.jna" % "jna" % "4.5.1"
)

javacOptions ++= Seq("-Xmx1024M")
