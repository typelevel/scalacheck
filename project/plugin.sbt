addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.13.0")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.12")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.0")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.3.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.4")
val sbtTypelevelVersion = "0.4.18"
addSbtPlugin("org.typelevel" % "sbt-typelevel-ci-release" % sbtTypelevelVersion)
addSbtPlugin("org.typelevel" % "sbt-typelevel-settings" % sbtTypelevelVersion)
