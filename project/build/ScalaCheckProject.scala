import sbt._

class ScalaCheckProject(info: ProjectInfo) extends DefaultProject(info) {

  override def crossScalaVersions = Set("2.7.2", "2.7.3", "2.7.4", "2.7.5", "2.7.6")

  override def managedStyle = ManagedStyle.Maven

  val publishTo = "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/snapshots/"

  Credentials(Path.userHome / ".ivy2" / ".credentials", log)

  override def packageDocsJar = defaultJarPath("-javadoc.jar")
  override def packageSrcJar= defaultJarPath("-sources.jar")
  val sourceArtifact = Artifact(artifactID, "src", "jar", Some("sources"), Nil, None)
  val docsArtifact = Artifact(artifactID, "docs", "jar", Some("javadoc"), Nil, None)
  override def packageToPublishActions = super.packageToPublishActions ++ Seq(packageDocs, packageSrc)
}
