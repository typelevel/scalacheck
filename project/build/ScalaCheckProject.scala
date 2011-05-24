import sbt._

class ScalaCheckProject(info: ProjectInfo) extends DefaultProject(info) {

  override def managedStyle = ManagedStyle.Maven
  override def packageDocsJar = defaultJarPath("-javadoc.jar")
  override def packageSrcJar= defaultJarPath("-sources.jar")
  override def packageToPublishActions =
    super.packageToPublishActions ++ Seq(packageDocs, packageSrc)
  override def deliverScalaDependencies = Nil
  override def documentOptions = Nil

  val scalaToolsReleases = "Scala Tools Releases" at
    "http://nexus.scala-tools.org/content/repositories/releases/"

  val scalaToolsSnapshots = "Scala Tools Snapshots" at
    "http://nexus.scala-tools.org/content/repositories/snapshots/"

  val publishTo = scalaToolsSnapshots

  val sourceArtifact = Artifact(artifactID, "src", "jar", Some("sources"), Nil, None)
  val docsArtifact = Artifact(artifactID, "docs", "jar", Some("javadoc"), Nil, None)

  val depTestInterface = "org.scala-tools.testing" %  "test-interface" % "0.5"

  Credentials(Path.userHome / ".ivy2" / ".credentials", log)
}
