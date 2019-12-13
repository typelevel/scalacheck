addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.6.1")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.0")

def env(name: String): Option[String] =
  Option(System.getenv(name))

def printAndDie(msg: String): Nothing = {
  println(msg)
  sys.error(msg)
}

// Update SCALAJS_VERSION in release.sh, as well
val scalaJSVersion = env("SCALAJS_VERSION") match {
  case Some("0.6.31") | None => "0.6.31"
  case Some("1.0.0-RC2") => "1.0.0-RC2"
  case Some(v) => printAndDie(s"unsupported scala.js version: $v")
}

addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion)

// Update SCALANATIVE_VERSION in release.sh, as well
val scalaNativeVersion = env("SCALANATIVE_VERSION") match {
  case Some("0.3.9") | None => "0.3.9"
  case Some("0.4.0-M2") => "0.4.0-M2"
  case Some(v) => printAndDie(s"unsupported scala native version: $v")
}

addSbtPlugin("org.scala-native" % "sbt-scala-native" % scalaNativeVersion)

scalacOptions += "-deprecation"
