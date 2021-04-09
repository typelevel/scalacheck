addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.8.1")

addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.5.1")

def env(name: String): Option[String] =
  Option(System.getenv(name))

def printAndDie(msg: String): Nothing = {
  println(msg)
  sys.error(msg)
}

addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.0")

addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.0")

addSbtPlugin("com.codecommit" % "sbt-github-actions" % "0.10.1")
