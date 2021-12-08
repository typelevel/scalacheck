import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

ThisBuild / baseVersion := "1.15"
ThisBuild / versionScheme := Some("pvp")

ThisBuild / organization := "io.vasilev"
ThisBuild / organizationName := "ScalaCheck"
ThisBuild / organizationHomepage := Some(url("https://scalacheck.org"))
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"

ThisBuild / developers := List(
  Developer(
    id    = "rickynils",
    name  = "Rickard Nilsson",
    email = "rickynils@gmail.com",
    url   = url("https://github.com/rickynils")
  )
)

ThisBuild / homepage := Some(url("https://scalacheck.org"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/typelevel/scalacheck"),
    "scm:git:git@github.com:typelevel/scalacheck.git"
  )
)

ThisBuild / licenses := Seq("BSD-3-Clause" -> url("https://opensource.org/licenses/BSD-3-Clause"))

val Scala212 = "2.12.15"
val Scala213 = "2.13.7"
val Scala30 = "3.0.2"
val Scala31 = "3.1.0"

ThisBuild / crossScalaVersions := Seq(Scala31, Scala30, Scala212, Scala213)

ThisBuild / githubWorkflowArtifactUpload := false
ThisBuild / githubWorkflowPublishTargetBranches := Seq()

val PrimaryOS = "ubuntu-latest"
ThisBuild / githubWorkflowOSes := Seq(PrimaryOS)

val Java8 = JavaSpec.temurin("8")
val Java11 = JavaSpec.temurin("11")

ThisBuild / githubWorkflowJavaVersions := Seq(Java8, Java11)

ThisBuild / githubWorkflowBuildMatrixAdditions ++= Map(
  "platform" -> List("coreJVM"),
  "workers" -> List("1", "4")
)

ThisBuild / githubWorkflowBuildMatrixInclusions ++=
  crossScalaVersions.value map { scala =>
    MatrixInclude(
      Map("os" -> PrimaryOS, "java" -> Java8.render, "scala" -> scala),
      Map("platform" -> "coreJS", "workers" -> "1"))
  }

ThisBuild / githubWorkflowBuildMatrixInclusions ++=
  crossScalaVersions.value.filter(_.startsWith("2.")) map { scala =>
    MatrixInclude(
      Map(
        "os" -> PrimaryOS,
        "scala" -> scala,
        "java" -> Java8.render),
      Map("platform" -> "coreNative", "workers" -> "1"))
  }

ThisBuild / githubWorkflowBuildPreamble +=
  WorkflowStep.Run(
    List("sudo apt install clang libunwind-dev libgc-dev libre2-dev"),
    name = Some("Setup scala native dependencies"),
    cond = Some("matrix.platform == 'native'"))

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Run(
    List("./tools/travis-script.sh"),
    name = Some("Run the build script"),
    env = Map(
      "PLATFORM" -> "${{ matrix.platform }}",
      "TRAVIS_SCALA_VERSION" -> "${{ matrix.scala }}",
      "WORKERS" -> "${{ matrix.workers }}")))

ThisBuild / githubWorkflowAddedJobs ++= Seq(
  WorkflowJob(
    "examples",
    "Examples",
    githubWorkflowJobSetup.value.toList ::: List(
      WorkflowStep.Run(
        List(
          "cd examples",
          "for d in */ ; do cd \"$d\" && sbt test:compile && cd ../ ; done"),
        name = Some("Build examples"))),
    javas = List(Java8),
    scalas = List(crossScalaVersions.value.last)),

  WorkflowJob(
    "bench",
    "Bench",
    githubWorkflowJobSetup.value.toList ::: List(
      WorkflowStep.Sbt(
        List("bench/jmh:run -p genSize=0 -p seedCount=0 -bs 1 -wi 0 -i 1 -f 0 -t 1 -r 0 org.scalacheck.bench.GenBench"),
        name = Some("Build benchmark suite"))),
    javas = List(Java8),
    scalas = List(crossScalaVersions.value.last)))

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
    moduleName := "scalacheck",

    Compile / sourceGenerators += task {
      val dir = (Compile / sourceManaged).value / "org" / "scalacheck"
      codegen.genAll.map { s =>
        val f = dir / s.name
        IO.write(f, s.code)
        f
      }
    },

    Compile / packageSrc / mappings ++= (Compile / managedSources).value.map{ f =>
      // to merge generated sources into sources.jar as well
      (f, f.relativeTo((Compile / sourceManaged).value).get.getPath)
    },

    headerSources / excludeFilter := HiddenFileFilter || "*.scala",

    scalacOptions ~= (_ filterNot (_ == "-Xfatal-warnings")),

    // HACK: without these lines, the console is basically unusable,
    // since all imports are reported as being unused (and then become
    // fatal errors).
    Compile / console / scalacOptions ~= {_.filterNot("-Ywarn-unused-import" == _)},
    Test / console / scalacOptions := (Compile / console / scalacOptions).value,

    // don't use fatal warnings in tests
    Test / scalacOptions ~= (_ filterNot (_ == "-Xfatal-warnings")),

    autoAPIMappings := true,
    // Mima signature checking stopped working after 3.0.2 upgrade, see #834
    mimaReportSignatureProblems := (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) => false
      case _ => true
    }),
    mimaPreviousArtifacts := Set("org.scalacheck" %%% "scalacheck" % "1.15.4"),

    // Don't publish for Scala 3.1 or later, only from 3.0
    publish / skip := (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, x)) if x > 0 => true
      case _                     => false
    }),

    publishMavenStyle := true,

    publishArtifact := true,

    Test / publishArtifact := false,

    pomIncludeRepository := { _ => false },
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
  )
  .jsSettings(
    libraryDependencies +=
      ("org.scala-js" %% "scalajs-test-interface" % scalaJSVersion).cross(CrossVersion.for3Use2_13)
  )
  .nativeSettings(
    libraryDependencies += "org.scala-native" %%% "test-interface" % nativeVersion,
    crossScalaVersions := (ThisBuild / crossScalaVersions).value.filter(_.startsWith("2.")),
    mimaPreviousArtifacts := Set()
  )

lazy val bench = project
  .in(file("bench"))
  .dependsOn(core.jvm)
  .enablePlugins(JmhPlugin, NoPublishPlugin)
  .settings(
    name := "scalacheck-bench",
    fork := true,
    headerSources / excludeFilter := HiddenFileFilter || "*.scala"
  )
