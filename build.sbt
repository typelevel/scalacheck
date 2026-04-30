val Scala212 = "2.12.21"
val Scala213 = "2.13.18"
val Scala3 = "3.3.7"

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

ThisBuild / tlBaseVersion := "1.19"
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

// publish website from this branch
ThisBuild / tlSitePublishBranch := Some("main")

import laika.config.{LinkConfig, MessageFilters, SyntaxHighlighting, TargetDefinition, Version, Versions}
import laika.helium.Helium
import laika.helium.config.{Favicon, HeliumIcon, IconLink, LinkPanel, ReleaseInfo, Teaser, TextLink}
import laika.ast.Path.Root
import laika.ast.Image
import laika.format.Markdown
import laika.theme.config.Color

val scalaCheckRepo = "https://github.com/typelevel/scalacheck"
val latestVersion = "1.19.0"

lazy val docs = project.in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .settings(
    laikaExtensions ++= Seq(
      Markdown.GitHubFlavor,
      SyntaxHighlighting
    ),
    laikaTheme :=
      Helium.defaults
        .site.landingPage(
          logo = Some(Image.internal(Root / "images/logo_forall_transparent.png")),
          title = Some("ScalaCheck"),
          subtitle = Some("Property Based Testing for Scala"),
          latestReleases = Seq(
            Some(ReleaseInfo("Latest Stable Release", tlBaseVersion.value)),
            tlLatestPreReleaseVersion.value.map(s => ReleaseInfo("Latest Milestone Release", s))
          ).flatten,
          linkPanel = Some(LinkPanel(
            "Documentation",
            TextLink.internal(Root / "main.md", "Introduction"),
            TextLink.internal(Root / "resources.md", "Resources"),
            TextLink.internal(Root / "download.md", "Download"),
            TextLink.internal(Root / "userguide.md", "User Guide"),
            TextLink.internal(Root / "sources.md", "Sources"),
            TextLink.internal(Root / "api.md", "API"),
            TextLink.internal(Root / "old_releases.md", "Old Releases")
          )),
          projectLinks = Seq(
            TextLink.external("https://github.com/typelevel/scalacheck", "Source on Github"),
            TextLink.external(
              "https://discord.com/channels/632277896739946517/841617753513263144",
              "Discussions on Discord"),
            TextLink.external("https://github.com/typelevel/scalacheck/discussions", "Discussions on GitHub"),
            TextLink.external("https://github.com/typelevel/scalacheck/issues", "Issues on GitHub")
          ),
          license = Some("BSD-3-Clause"),
          teasers = Seq(
            Teaser(
              "Thorough",
              "Write a single property and let ScalaCheck execute it hundreds or thousands of times. Instead of a handful of examples, you explore a wide space of possible inputs automatically. This gives you confidence that your code behaves correctly beyond the obvious cases."
            ),
            Teaser(
              "Generative",
              "Stop hand-crafting test data and let ScalaCheck generate it for you. From simple values to complex domain objects, generators produce diverse scenarios effortlessly. Your tests evolve from examples into full explorations of behavior."
            ),
            Teaser(
              "Exploratory",
              "ScalaCheck helps you uncover edge cases you didn’t know existed. By exploring random and structured inputs, it reveals hidden assumptions in your code. Many bugs surface not because you expected them, but because ScalaCheck went looking."
            ),
            Teaser(
              "Minimal",
              "When a test fails, ScalaCheck doesn’t just stop, it shrinks the input. It searches for the smallest, simplest example that still reproduces the failure. Debugging becomes faster because you’re working with minimal, focused data."
            ),
            Teaser(
              "Expressive",
              "Tests read like laws or truths about your system. Instead of imperative scripts, you write clear statements of intent. This makes your test suite easier to understand and communicate."
            )
          )
        )
        .site.themeColors(
          primary = Color.hex("818589"),
          primaryLight = Color.hex("FFF6CC"),
          primaryMedium = Color.hex("8A7A3B"),
          secondary = Color.hex("2B2B2B"),
          text = Color.hex("818589"),
          background = Color.hex("FFFDF5"),
          bgGradient = (Color.hex("FFFDF5"), Color.hex("FFF3BF"))
        )
        .site.favIcons(
          Favicon.internal(Root / "images/favicon.ico", sizes = "32x32")
        )
        .site.topNavigationBar(
          homeLink = IconLink.internal(Root / "main.md", HeliumIcon.home)
        )
        .site.footer(Image.internal(Root / "images/logo_forall_h30.png"))
        .build,

    laikaConfig := {

      LaikaConfig
        .defaults
        .withMessageFilters(MessageFilters.forVisualDebugging)
        .withConfigValue(
          LinkConfig.empty.addTargets(
            TargetDefinition.external("ScalaCheck Repository", scalaCheckRepo),
            TargetDefinition.external("ScalaCheck Bug Reports", s"$scalaCheckRepo/issues"),
            TargetDefinition.external("ScalaCheck Discussions on GitHub", s"$scalaCheckRepo/discussions"),
            TargetDefinition.external("report an issue", s"$scalaCheckRepo/issues"),
            TargetDefinition.external("submit a pull request", s"$scalaCheckRepo/pull")
          )
        )
    }
  )

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
      "org.apache.commons" % "commons-lang3" % "3.20.0" % Test,
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
    tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "1.18.0").toMap
  )

lazy val bench = project.in(file("bench"))
  .dependsOn(core.jvm)
  .settings(
    name := "scalacheck-bench",
    fork := true
  )
  .enablePlugins(NoPublishPlugin, JmhPlugin)
