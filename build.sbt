val Scala212 = "2.12.18"
val Scala213 = "2.13.11"
val Scala3 = "3.3.1"

name := "scalacheck"
ThisBuild / organization := "org.scalacheck"
ThisBuild / organizationName := "Typelevel"
ThisBuild / homepage := Some(url("http://www.scalacheck.org"))
ThisBuild / licenses := Seq("BSD-3-Clause" -> url("https://opensource.org/licenses/BSD-3-Clause"))
ThisBuild / developers := List(
  Developer(
    id = "rickynils",
    name = "Rickard Nilsson",
    email = "rickynils@gmail.com",
    url = url("https://github.com/rickynils")
  )
)

ThisBuild / tlCiReleaseBranches := Seq("main")

ThisBuild / crossScalaVersions := Seq(Scala3, Scala212, Scala213)
val Java8 = JavaSpec.temurin("8")
ThisBuild / githubWorkflowJavaVersions := Seq(Java8, JavaSpec.temurin("11"))
ThisBuild / githubWorkflowBuildMatrixAdditions += "workers" -> List("1", "4")

ThisBuild / githubWorkflowBuildMatrixExclusions ++=
  List(
    MatrixExclude(Map("project" -> "rootJS", "workers" -> "4")),
    MatrixExclude(Map("project" -> "rootNative", "workers" -> "4"))
  )

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
    scalas = Nil
  ),
  WorkflowJob(
    "bench",
    "Bench",
    githubWorkflowJobSetup.value.toList ::: List(
      WorkflowStep.Sbt(
        List("bench/jmh:run -p genSize=0 -p seedCount=0 -bs 1 -wi 0 -i 1 -f 0 -t 1 -r 0 org.scalacheck.bench.GenBench"),
        name = Some("Build benchmark suite"))),
    javas = List(Java8),
    scalas = Nil
  )
)

ThisBuild / tlBaseVersion := "1.17"
ThisBuild / tlMimaPreviousVersions ++= Set(
  // manually added because tags are not v-prefixed
  "1.14.0",
  "1.14.1",
  "1.14.2",
  "1.14.3",
  "1.15.0",
  "1.15.1",
  "1.15.2",
  "1.15.3",
  "1.15.4"
)
ThisBuild / tlVersionIntroduced := Map("3" -> "1.15.3")

lazy val root = tlCrossRootProject.aggregate(core, bench)
  .settings(
    Compile / headerSources ++= Seq(
      "codegen.scala",
      "CustomHeaderPlugin.scala"
    ).map {
      BuildPaths.projectStandard((ThisBuild / baseDirectory).value) / _
    }
  )

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("core"))
  .settings(
    name := "scalacheck",
    Compile / sourceGenerators += task {
      val dir = (Compile / sourceManaged).value / "org" / "scalacheck"
      codegen.genAll.map { s =>
        val f = dir / s.name
        IO.write(f, s.code)
        f
      }
    },
    tlFatalWarnings := false // TODO
  )
  .jvmSettings(
    Test / fork := true,
    libraryDependencies ++= Seq(
      "org.apache.commons" % "commons-lang3" % "3.13.0" % Test,
      "org.scala-sbt" % "test-interface" % "1.0"
    )
  )
  .jsSettings(
    libraryDependencies +=
      ("org.scala-js" %% "scalajs-test-interface" % scalaJSVersion).cross(CrossVersion.for3Use2_13),
    tlVersionIntroduced ++= List("2.12", "2.13").map(_ -> "1.14.3").toMap
  )
  .nativeSettings(
    libraryDependencies ++= Seq(
      "org.scala-native" %%% "test-interface" % nativeVersion
    ),
    tlVersionIntroduced ++=
      List("2.12", "2.13").map(_ -> "1.15.2").toMap ++ Map("3" -> "1.16.0")
  )

lazy val bench = project.in(file("bench"))
  .dependsOn(core.jvm)
  .settings(
    name := "scalacheck-bench",
    fork := true
  )
  .enablePlugins(NoPublishPlugin, JmhPlugin)
