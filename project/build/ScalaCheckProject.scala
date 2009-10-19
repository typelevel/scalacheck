import sbt._

class ScalaCheckProject(info: ProjectInfo) extends DefaultProject(info) {

  override def crossScalaVersions = Set("2.7.2", "2.7.3", "2.7.4", "2.7.5", "2.7.6")

  override def managedStyle = ManagedStyle.Maven

  override def packageDocsJar = defaultJarPath("-javadoc.jar")
  override def packageSrcJar= defaultJarPath("-sources.jar")

  override def packageToPublishActions = super.packageToPublishActions ++ Seq(packageDocs, packageSrc)

  override def deliverScalaDependencies = Nil

  val publishTo = "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/snapshots/"

  val sourceArtifact = Artifact(artifactID, "src", "jar", Some("sources"), Nil, None)
  val docsArtifact = Artifact(artifactID, "docs", "jar", Some("javadoc"), Nil, None)

  Credentials(Path.userHome / ".ivy2" / ".credentials", log)

}
