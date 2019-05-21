sourceDirectory := file("dummy source directory")

val scalaMajorVersion = SettingKey[Int]("scalaMajorVersion")

scalaVersionSettings

// When bumping to 1.14.1, remember to set mimaPreviousArtifacts to 1.14.0
lazy val versionNumber = "1.14.0"

lazy val isRelease = true

lazy val travisCommit = Option(System.getenv().get("TRAVIS_COMMIT"))

lazy val scalaVersionSettings = Seq(
  scalaVersion := "2.12.6",
  crossScalaVersions := Seq("2.10.7", "2.11.12", "2.13.0-RC2", scalaVersion.value),
  scalaMajorVersion := {
    val v = scalaVersion.value
    CrossVersion.partialVersion(v).map(_._2.toInt).getOrElse {
      throw new RuntimeException(s"could not get Scala major version from $v")
    }
  }
)

lazy val sharedSettings = MimaSettings.settings ++ scalaVersionSettings ++ Seq(

  name := "scalacheck",

  version := {
    val suffix =
      if (isRelease) ""
      else travisCommit.map("-" + _.take(7)).getOrElse("") + "-SNAPSHOT"
    versionNumber + suffix
  },

  isSnapshot := !isRelease,

  organization := "org.scalacheck",

  licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php")),

  homepage := Some(url("http://www.scalacheck.org")),

  credentials ++= (for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    username, password
  )).toSeq,

  unmanagedSourceDirectories in Compile += (baseDirectory in LocalRootProject).value / "src" / "main" / "scala",

  unmanagedSourceDirectories in Compile += {
    val s = if (scalaMajorVersion.value >= 13) "+" else "-"
    (baseDirectory in LocalRootProject).value / "src" / "main" / s"scala-2.13$s"
  },

  unmanagedSourceDirectories in Test += (baseDirectory in LocalRootProject).value / "src" / "test" / "scala",

  resolvers += "sonatype" at "https://oss.sonatype.org/content/repositories/releases",

  javacOptions += "-Xmx1024M",

  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-Xfuture",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen") ++ {
    val modern = Seq("-Xlint:-unused", "-Ywarn-unused:-patvars,-implicits,-locals,-privates,-explicits")
    val removed = Seq("-Ywarn-inaccessible", "-Ywarn-nullary-override", "-Ywarn-nullary-unit")
    val removedModern = Seq("-Ywarn-infer-any", "-Ywarn-unused-import")
    scalaMajorVersion.value match {
      case 10 => Seq("-Xfatal-warnings", "-Xlint") ++ removed
      case 11 => Seq("-Xfatal-warnings", "-Xlint", "-Ywarn-infer-any", "-Ywarn-unused-import") ++ removed
      case 12 => "-Xfatal-warnings" +: (modern ++ removed ++ removedModern)
      case 13 => modern
    }
  },

  // HACK: without these lines, the console is basically unusable,
  // since all imports are reported as being unused (and then become
  // fatal errors).
  scalacOptions in (Compile, console) ~= {_.filterNot("-Ywarn-unused-import" == _)},
  scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value,

  // don't use fatal warnings in tests
  scalacOptions in Test ~= (_ filterNot (_ == "-Xfatal-warnings")),

  mimaPreviousArtifacts := {
    // TODO: re-enable MiMa for 2.13 once there is a release out
    if (scalaMajorVersion.value == 13) Set()
    else Set("org.scalacheck" %% "scalacheck" % "1.14.0")
  },

  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    val (name, path) = if (isSnapshot.value) ("snapshots", "content/repositories/snapshots")
                       else ("releases", "service/local/staging/deploy/maven2")
    Some(name at nexus + path)
  },

  publishMavenStyle := true,

  // Travis should only publish snapshots
  publishArtifact := !(isRelease && travisCommit.isDefined),

  publishArtifact in Test := false,

  pomIncludeRepository := { _ => false },

  pomExtra := {
    <scm>
      <url>https://github.com/rickynils/scalacheck</url>
      <connection>scm:git:git@github.com:rickynils/scalacheck.git</connection>
    </scm>
    <developers>
      <developer>
        <id>rickynils</id>
        <name>Rickard Nilsson</name>
      </developer>
    </developers>
  }
)

lazy val js = project.in(file("js"))
  .settings(sharedSettings: _*)
  .settings(
    scalaJSStage in Global := FastOptStage,
    libraryDependencies += "org.scala-js" %% "scalajs-test-interface" % scalaJSVersion,
    mimaPreviousArtifacts := {
      val isScalaJSMilestone: Boolean =
        Option(System.getenv("SCALAJS_VERSION")).filter(_.startsWith("1.0.0-M")).isDefined
      // TODO: re-enable MiMa for 2.13 once there is a release out
      if (scalaMajorVersion.value == 13 || isScalaJSMilestone) Set()
      else Set("org.scalacheck" %%% "scalacheck" % "1.14.0")
    },
    // because Scala.js deprecated TestUtils but we haven't worked around that yet,
    // see https://github.com/rickynils/scalacheck/pull/435#issuecomment-430405390
    scalacOptions ~= (_ filterNot (_ == "-Xfatal-warnings"))
  )
  .enablePlugins(ScalaJSPlugin)

lazy val jvm = project.in(file("jvm"))
  .settings(sharedSettings: _*)
  .settings(
    fork in Test := true,
    libraryDependencies += "org.scala-sbt" %  "test-interface" % "1.0"
  )

lazy val native = project.in(file("native"))
  .settings(sharedSettings: _*)
  .settings(
    doc in Compile := (doc in Compile in jvm).value,
    scalaVersion := "2.11.12",
    libraryDependencies ++= Seq(
      "org.scala-native" %% "test-interface_native0.3" % nativeVersion
    )
  )
  .enablePlugins(ScalaNativePlugin)
