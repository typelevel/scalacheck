name := "commands-redis"

scalaVersion := "2.13.5"

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.15.1" % "test",
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  "net.debasishg" %% "redisclient" % "3.6"
)
