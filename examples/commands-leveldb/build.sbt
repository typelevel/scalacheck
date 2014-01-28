name := "commands-leveldb"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "org.scala-sbt" %  "test-interface" % "1.0",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
)

javacOptions ++= Seq("-Xmx1024M")

// JNI workaround, http://stackoverflow.com/questions/19425613/unsatisfiedlinkerror-with-native-library-under-sbt
fork := true
