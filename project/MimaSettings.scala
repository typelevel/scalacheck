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
  )

  private def removedPrivateClasses = Seq(
  )

  private def otherProblems = Seq(
  )

}
