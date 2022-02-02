sourceDirectory := file("dummy source directory")

val Scala212 = "2.12.15"
val Scala213 = "2.13.8"
val Scala30 = "3.0.2"
val Scala31 = "3.1.1"

ThisBuild / crossScalaVersions := Seq(Scala31, Scala30, Scala212, Scala213)
ThisBuild / scalaVersion := (ThisBuild / crossScalaVersions).value.last

ThisBuild / githubWorkflowPublishTargetBranches := Seq()

val PrimaryOS = "ubuntu-latest"
ThisBuild / githubWorkflowOSes := Seq(PrimaryOS)

val Java8 = JavaSpec.temurin("8")
val Java11 = JavaSpec.temurin("11")

ThisBuild / githubWorkflowJavaVersions := Seq(Java8, Java11)

// we don't need this since we aren't publishing
ThisBuild / githubWorkflowArtifactUpload := false

ThisBuild / githubWorkflowBuildMatrixAdditions += "platform" -> List("jvm")
ThisBuild / githubWorkflowBuildMatrixAdditions += "workers" -> List("1", "4")

ThisBuild / githubWorkflowBuildMatrixInclusions ++=
  crossScalaVersions.value map { scala =>
    MatrixInclude(
      Map("os" -> PrimaryOS, "java" -> Java8.render, "scala" -> scala),
      Map("platform" -> "js", "workers" -> "1"))
  }

ThisBuild / githubWorkflowBuildMatrixInclusions ++=
  crossScalaVersions.value.filter(_.startsWith("2.")) map { scala =>
    MatrixInclude(
      Map(
        "os" -> PrimaryOS,
        "scala" -> scala,
        "java" -> Java8.render),
      Map("platform" -> "native", "workers" -> "1"))
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

lazy val versionNumber = "1.15.5"

def env(name: String): Option[String] =
  Option(System.getenv(name))

val isRelease = env("IS_RELEASE").exists(_ == "true")

ThisBuild / versionScheme := Some("pvp")

lazy val sharedSettings = MimaSettings.settings ++ Seq(

  name := "scalacheck",

  version := {
    val suffix =
      if (isRelease) ""
      else "-SNAPSHOT"
    versionNumber + suffix
  },

  isSnapshot := !isRelease,

  organization := "org.scalacheck",

  licenses := Seq("BSD 3-clause" -> url("https://opensource.org/licenses/BSD-3-Clause")),

  homepage := Some(url("http://www.scalacheck.org")),

  credentials ++= (for {
    username <- env("SONATYPE_USERNAME")
    password <- env("SONATYPE_PASSWORD")
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    username, password
  )).toSeq,

  Compile / unmanagedSourceDirectories += (LocalRootProject / baseDirectory).value / "src" / "main" / "scala",

  Compile / packageSrc / mappings ++= (Compile / managedSources).value.map{ f =>
    // to merge generated sources into sources.jar as well
    (f, f.relativeTo((Compile / sourceManaged).value).get.getPath)
  },

  Compile / sourceGenerators += task {
    val dir = (Compile / sourceManaged).value / "org" / "scalacheck"
    codegen.genAll.map { s =>
      val f = dir / s.name
      IO.write(f, s.code)
      f
    }
  },

  Compile / unmanagedSourceDirectories += {
    val s = CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3,  _)) => "scala-2.13+"
      case Some((2, 13)) => "scala-2.13+"
      case _             => "scala-2.13-"
    }
    (LocalRootProject / baseDirectory).value / "src" / "main" / s
  },

  Test / unmanagedSourceDirectories += (LocalRootProject / baseDirectory).value / "src" / "test" / "scala",

  resolvers += "sonatype" at "https://oss.sonatype.org/content/repositories/releases",

  // 2.11 - 2.13
  scalacOptions ++= {
    def mk(r: Range)(strs: String*): Int => Seq[String] =
      (n: Int) => if (r.contains(n)) strs else Seq.empty

    val groups: Seq[Int => Seq[String]] = Seq(
      mk(12 to 12)("-Ywarn-inaccessible", "-Ywarn-nullary-override",
        "-Ywarn-nullary-unit", "-Xfuture", "-Xfatal-warnings", "-deprecation",
        "-Ywarn-infer-any", "-Ywarn-unused-import"),
      mk(12 to 13)("-encoding", "UTF-8", "-feature", "-unchecked",
        "-Ywarn-dead-code", "-Ywarn-numeric-widen", "-Xlint:-unused",
        "-Ywarn-unused:-patvars,-implicits,-locals,-privates,-explicits"))

    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) => groups.flatMap(f => f(n.toInt))
      case _            => Seq("-language:Scala2")
    }
  },

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

  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    val (name, path) = if (isSnapshot.value) ("snapshots", "content/repositories/snapshots")
                       else ("releases", "service/local/staging/deploy/maven2")
    Some(name at nexus + path)
  },

  publishMavenStyle := true,

  publishArtifact := true,

  Test / publishArtifact := false,

  pomIncludeRepository := { _ => false },

  scmInfo := Some(
    ScmInfo(
      url("https://github.com/typelevel/scalacheck"),
      "scm:git:git@github.com:typelevel/scalacheck.git"
    )
  ),
  developers := List(
    Developer(
      id    = "rickynils",
      name  = "Rickard Nilsson",
      email = "rickynils@gmail.com",
      url   = url("https://github.com/rickynils")
    )
  )
)

lazy val js = project.in(file("js"))
  .settings(sharedSettings: _*)
  .settings(
    Global / scalaJSStage := FastOptStage,
    libraryDependencies +=
      ("org.scala-js" %% "scalajs-test-interface" % scalaJSVersion).cross(CrossVersion.for3Use2_13)
  )
  .enablePlugins(ScalaJSPlugin)

lazy val jvm = project.in(file("jvm"))
  .settings(sharedSettings: _*)
  .settings(
    Test / fork := {
      // Serialization issue in 2.13 and later
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3,  _)) => true
        case Some((2, 13)) => true
        case _             => false
      }
    },
    Test / unmanagedSourceDirectories += {
      val s = CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3,  _)) => "scala-2.13+"
        case Some((2, 13)) => "scala-2.13+"
        case _             => "scala-2.13-"
      }
      baseDirectory.value / "src" / "test" / s
    },
    libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.12.0" % "test",
    libraryDependencies += "org.scala-sbt" %  "test-interface" % "1.0"
  )

lazy val native = project.in(file("native"))
  .settings(sharedSettings: _*)
  .settings(
    scalaVersion := Scala212,
    crossScalaVersions := Seq(Scala212, Scala213),
    // TODO: re-enable MiMa for native once published
    mimaPreviousArtifacts := Set(),
    libraryDependencies ++= Seq(
      "org.scala-native" %%% "test-interface" % nativeVersion
    )
  )
  .enablePlugins(ScalaNativePlugin)

lazy val bench = project.in(file("bench"))
  .dependsOn(jvm)
  .settings(
    name := "scalacheck-bench",
    fork := true,
    publish / skip := true,
    mimaPreviousArtifacts := Set.empty,
  )
  .enablePlugins(JmhPlugin)
