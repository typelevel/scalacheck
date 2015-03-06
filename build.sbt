import com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact

sourceDirectory := file("dummy source directory")

lazy val sharedSettings = mimaDefaultSettings ++ Seq(

  name := "scalacheck",

  version := "1.12.3-SNAPSHOT",

  organization := "org.scalacheck",

  licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php")),

  homepage := Some(url("http://www.scalacheck.org")),

  scalaVersion := "2.11.5",

  crossScalaVersions := Seq("2.10.4", "2.11.5"),

  libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _),

  unmanagedSourceDirectories in Compile += {
    if (scalaVersion.value.startsWith("2.11."))
      (baseDirectory in LocalRootProject).value / "src" / "main" / "scala-2.11"
    else if (scalaVersion.value.startsWith("2.10."))
      (baseDirectory in LocalRootProject).value / "src" / "main" / "scala-2.10"
    else ???
  },

  unmanagedSourceDirectories in Test ++= {
    if (scalaVersion.value.startsWith("2.11."))
      Seq((baseDirectory in LocalRootProject).value / "src" / "test" / "scala-2.11")
    else if (scalaVersion.value.startsWith("2.10."))
      Seq()
    else ???
  },

  previousArtifact := Some("org.scalacheck" % "scalacheck_2.11" % "1.12.1"),

  unmanagedSourceDirectories in Compile += (baseDirectory in LocalRootProject).value / "src" / "main" / "scala",

  unmanagedSourceDirectories in Test += (baseDirectory in LocalRootProject).value / "src" / "test" / "scala",

  resolvers += "sonatype" at "https://oss.sonatype.org/content/repositories/releases",

  javacOptions += "-Xmx1024M",

  scalacOptions ++= Seq("-deprecation", "-feature"),

  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    val (name, path) = if (isSnapshot.value) ("snapshots", "content/repositories/snapshots")
                       else ("releases", "service/local/staging/deploy/maven2")
    Some(name at nexus + path)
  },

  publishMavenStyle := true,

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
    libraryDependencies += "org.scala-js" %% "scalajs-test-interface" % scalaJSVersion
  )
  .enablePlugins(ScalaJSPlugin)

lazy val jvm = project.in(file("jvm"))
  .settings(sharedSettings: _*)
  .settings(
    libraryDependencies += "org.scala-sbt" %  "test-interface" % "1.0",
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % scalaJSVersion % "provided"
  )
