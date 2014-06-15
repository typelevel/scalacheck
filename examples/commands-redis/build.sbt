name := "commands-redis"

scalaVersion := "2.11.1"

resolvers += 
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.12.0-SNAPSHOT",
  "org.slf4j" % "slf4j-simple" % "1.7.7",
  "net.debasishg" %% "redisclient" % "2.13"
)
