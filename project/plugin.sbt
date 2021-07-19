addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.9.2")

addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.6.0")

def env(name: String): Option[String] =
  Option(System.getenv(name))

def printAndDie(msg: String): Nothing = {
  println(msg)
  sys.error(msg)
}

addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.0")

addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.3")

addSbtPlugin("com.codecommit" % "sbt-github-actions" % "0.12.0")
