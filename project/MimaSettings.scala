object MimaSettings {

  import sbt._
  import com.typesafe.tools.mima
  import mima.core._
  import ProblemFilters.exclude
  import mima.plugin.MimaKeys.mimaBinaryIssueFilters
  import mima.plugin.MimaPlugin.mimaDefaultSettings

  lazy val settings = mimaDefaultSettings ++ Seq(
    mimaBinaryIssueFilters :=
      removedPrivateMethods.map(exclude[DirectMissingMethodProblem](_)) ++
      newMethods.map(exclude[ReversedMissingMethodProblem](_)) ++
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
    exclude[DirectAbstractMethodProblem]("org.scalacheck.ScalaCheckRunner.args"),
    exclude[DirectAbstractMethodProblem]("org.scalacheck.ScalaCheckRunner.loader"),
    exclude[DirectAbstractMethodProblem]("org.scalacheck.ScalaCheckRunner.params"),
    exclude[ReversedAbstractMethodProblem]("org.scalacheck.ScalaCheckRunner.args"),
    exclude[ReversedAbstractMethodProblem]("org.scalacheck.ScalaCheckRunner.loader"),
    exclude[ReversedAbstractMethodProblem]("org.scalacheck.ScalaCheckRunner.params"),
    exclude[ReversedMissingMethodProblem]("org.scalacheck.util.CmdLineParser.parseArgs")
  )

}
