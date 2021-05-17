name := "commands-redis"

scalaVersion := "2.13.6"

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.15.3" % Test,
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  "net.debasishg" %% "redisclient" % "3.10"
)
