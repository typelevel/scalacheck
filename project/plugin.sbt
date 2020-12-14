addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.8.1")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.1.1")

addSbtPlugin("ch.epfl.lamp" % "sbt-dotty" % "0.5.0")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.3.1")

def env(name: String): Option[String] =
  Option(System.getenv(name))

def printAndDie(msg: String): Nothing = {
  println(msg)
  sys.error(msg)
}

// Update SCALANATIVE_VERSION in release.sh, as well
val scalaNativeVersion = env("SCALANATIVE_VERSION") match {
  case Some("0.3.9") | Some("") | None => "0.3.9"
  case Some("0.4.0-M2") => "0.4.0-M2"
  case Some(v) => printAndDie(s"unsupported scala native version: $v")
}

addSbtPlugin("org.scala-native" % "sbt-scala-native" % scalaNativeVersion)

addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.0")

addSbtPlugin("com.codecommit" % "sbt-github-actions" % "0.9.5")
