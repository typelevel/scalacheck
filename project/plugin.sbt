addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.6.1")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.0")

// Update SCALAJS_VERSION in release.sh, as well
val scalaJSVersion =
  Option(System.getenv("SCALAJS_VERSION")).getOrElse("0.6.29")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion)

// Update SCALANATIVE_VERSION in release.sh, as well
val scalaNativeVersion =
  Option(System.getenv("SCALANATIVE_VERSION")).getOrElse("0.3.9")

addSbtPlugin("org.scala-native" % "sbt-scala-native" % scalaNativeVersion)

scalacOptions += "-deprecation"
