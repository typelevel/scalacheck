name := "commands-redis"

scalaVersion := "2.12.5"

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.13.5",
  "org.slf4j" % "slf4j-simple" % "1.7.7",
  "net.debasishg" %% "redisclient" % "3.4"
)
