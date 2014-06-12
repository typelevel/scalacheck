name := "commands-redis"

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "org.scala-sbt" %  "test-interface" % "1.0",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1",
  "org.slf4j" % "slf4j-simple" % "1.7.7",
  "net.debasishg" %% "redisclient" % "2.13"
)
