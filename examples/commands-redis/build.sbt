name := "commands-redis"

scalaVersion := "3.0.0"

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.15.4" % Test,
  "org.slf4j" % "slf4j-simple" % "1.7.30",
  ("net.debasishg" %% "redisclient" % "3.10").cross(CrossVersion.for3Use2_13)
)
