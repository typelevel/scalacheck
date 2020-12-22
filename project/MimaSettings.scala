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
    "org.scalacheck.commands.Commands.shrinkCommand",
    "org.scalacheck.commands.Commands.shrinkSequentialCommands",
    "org.scalacheck.commands.Commands.shrinkParallelCommands",
  )

  private def removedPrivateMethods = Seq(
  )

  private def removedPrivateClasses = Seq(
  )

  private def otherProblems = Seq(
  )

}
