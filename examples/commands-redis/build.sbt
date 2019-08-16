name := "commands-redis"

scalaVersion := "2.12.9"

val scalaCheckVersion = Option(System.getenv().get("TRAVIS_COMMIT"))
  .map("1.14.1-" + _.take(7) + "-SNAPSHOT")
  .getOrElse("1.14.0")

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % scalaCheckVersion % "test",
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  "net.debasishg" %% "redisclient" % "3.6"
)
