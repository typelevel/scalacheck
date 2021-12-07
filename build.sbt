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

ThisBuild / crossScalaVersions := Seq("2.13.7")

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
    )
  )
  .jsSettings(
    libraryDependencies +=
      ("org.scala-js" %% "scalajs-test-interface" % scalaJSVersion).cross(CrossVersion.for3Use2_13)
  )
  .nativeSettings(
    libraryDependencies += "org.scala-native" %%% "test-interface" % nativeVersion
  )
