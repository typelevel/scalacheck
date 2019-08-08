addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.3.0")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.2")

val scalaJSVersion =
  Option(System.getenv("SCALAJS_VERSION")).getOrElse("0.6.28")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion)

val scalaNativeVersion =
  Option(System.getenv("SCALANATIVE_VERSION")).getOrElse("0.3.9")

addSbtPlugin("org.scala-native" % "sbt-scala-native" % scalaNativeVersion)

scalacOptions += "-deprecation"
