import sbt._

class ScalaCheckProject(info: ProjectInfo) extends DefaultProject(info) {

  override def crossScalaVersions = List("2.8.0.Beta1-RC3", "2.8.0.Beta1-RC2")

  override def managedStyle = ManagedStyle.Maven

  override def packageDocsJar = defaultJarPath("-javadoc.jar")
  override def packageSrcJar= defaultJarPath("-sources.jar")

  override def packageToPublishActions = super.packageToPublishActions ++ Seq(/*packageDocs,*/ packageSrc)

  override def deliverScalaDependencies = Nil

  val publishTo = "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/snapshots/"

  val sourceArtifact = Artifact(artifactID, "src", "jar", Some("sources"), Nil, None)
  val docsArtifact = Artifact(artifactID, "docs", "jar", Some("javadoc"), Nil, None)

  val depTestInterface = "org.scala-tools.testing" %  "test-interface" % "0.2"

  Credentials(Path.userHome / ".ivy2" / ".credentials", log)

}
