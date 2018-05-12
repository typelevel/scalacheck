name := "commands-redis"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.14.0",
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  "net.debasishg" %% "redisclient" % "3.6"
)
