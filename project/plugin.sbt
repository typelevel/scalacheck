addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.2.0")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.1")

val scalaJSVersion =
  Option(System.getenv("SCALAJS_VERSION")).getOrElse("0.6.28")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion)

addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.3.7")

scalacOptions += "-deprecation"
