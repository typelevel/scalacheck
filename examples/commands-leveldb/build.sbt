name := "commands-leveldb"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "org.scala-sbt" %  "test-interface" % "1.0",
//  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
//  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.7"
//  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.6.1"
//  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.5"
//  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.4.1"
//  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.3"
//  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.2"
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.1"
)

javacOptions ++= Seq("-Xmx1024M")

// JNI workaround, http://stackoverflow.com/questions/19425613/unsatisfiedlinkerror-with-native-library-under-sbt
fork := true
