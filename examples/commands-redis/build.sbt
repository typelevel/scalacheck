name := "commands-redis"

scalaVersion := "3.3.8"

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.20.0" % Test,
  "org.slf4j" % "slf4j-simple" % "2.0.20",
  ("net.debasishg" %% "redisclient" % "3.10").cross(CrossVersion.for3Use2_13)
)
