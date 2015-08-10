object MimaSettings {

  import sbt._
  import com.typesafe.tools.mima
  import mima.core._
  import ProblemFilters.exclude
  import mima.plugin.MimaKeys.{binaryIssueFilters, previousArtifact}
  import mima.plugin.MimaPlugin.mimaDefaultSettings

  lazy val settings = mimaDefaultSettings ++ Seq(
    previousArtifact := Some("org.scalacheck" % "scalacheck_2.11" % "1.12.4"),
    binaryIssueFilters :=
      removedPrivateMethods.map(exclude[MissingMethodProblem](_)) ++
      newMethods.map(exclude[MissingMethodProblem](_)) ++
      removedPrivateClasses.map(exclude[MissingClassProblem](_)) ++
      otherProblems
  )

  private def newMethods = Seq(
  )

  private def removedPrivateMethods = Seq(
    "org.scalacheck.ScalaCheckRunner.remoteArgs",
    "org.scalacheck.ScalaCheckRunner.this",
    "org.scalacheck.util.CmdLineParser.parseArgs",
    "org.scalacheck.util.CmdLineParser.parseArgs"
  )

  private def removedPrivateClasses = Seq(
  )

  private def otherProblems = Seq(
    exclude[AbstractMethodProblem]("org.scalacheck.ScalaCheckRunner.args"),
    exclude[AbstractMethodProblem]("org.scalacheck.ScalaCheckRunner.loader"),
    exclude[AbstractMethodProblem]("org.scalacheck.ScalaCheckRunner.params"),
    exclude[AbstractMethodProblem]("org.scalacheck.ScalaCheckRunner.args"),
    exclude[AbstractMethodProblem]("org.scalacheck.ScalaCheckRunner.loader"),
    exclude[AbstractMethodProblem]("org.scalacheck.ScalaCheckRunner.params")
  )

}
