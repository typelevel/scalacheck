import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

ThisBuild / baseVersion := "1.15"

ThisBuild / organization := "org.scalacheck"
ThisBuild / organizationName := "ScalaCheck"
ThisBuild / organizationHomepage := Some(url("https://scalacheck.org"))

ThisBuild / developers := List(
  Developer(
    id    = "rickynils",
    name  = "Rickard Nilsson",
    email = "rickynils@gmail.com",
    url   = url("https://github.com/rickynils")
  )
)

val Scala212 = "2.12.15"
val Scala213 = "2.13.7"
val Scala30 = "3.0.2"
val Scala31 = "3.1.0"

ThisBuild / crossScalaVersions := Seq(Scala212, Scala213)

lazy val scalacheck = project
  .in(file("."))
  .enablePlugins(NoPublishPlugin)
  .aggregate(
    core.jvm,
    core.js,
    core.native
  )

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("core"))
  .settings(
    Compile / sourceGenerators += task {
      val dir = (Compile / sourceManaged).value / "org" / "scalacheck"
      codegen.genAll.map { s =>
        val f = dir / s.name
        IO.write(f, s.code)
        f
      }
    },
    headerSources / excludeFilter := HiddenFileFilter || "*.scala"
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.scala-sbt" % "test-interface" % "1.0",
      "org.apache.commons" % "commons-lang3" % "3.12.0" % Test
    ),
    Test / fork := {
      // Serialization issue in 2.13 and later
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 12)) => false
        case _             => true
      }
    },
    // 2.12 - 2.13
    scalacOptions := {
      def mk(r: Range)(strs: String*): Int => Seq[String] =
        (n: Int) => if (r.contains(n)) strs else Seq.empty

      val groups: Seq[Int => Seq[String]] = Seq(
        mk(12 to 12)("-Ywarn-inaccessible", "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit", "-Xfuture", "-deprecation",
          "-Ywarn-infer-any", "-Ywarn-unused-import"),
        mk(12 to 13)("-encoding", "UTF-8", "-feature", "-unchecked",
          "-Ywarn-dead-code", "-Ywarn-numeric-widen", "-Xlint:-unused",
          "-Ywarn-unused:-patvars,-implicits,-locals,-privates,-explicits"))

      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) => groups.flatMap(f => f(n.toInt))
        case _            => Seq("-language:Scala2")
      }
    }
  )
  .jsSettings(
    libraryDependencies +=
      ("org.scala-js" %% "scalajs-test-interface" % scalaJSVersion).cross(CrossVersion.for3Use2_13)
  )
  .nativeSettings(
    libraryDependencies += "org.scala-native" %%% "test-interface" % nativeVersion
  )
