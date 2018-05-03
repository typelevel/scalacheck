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
      methodTypeChanges.map(exclude[IncompatibleMethTypeProblem](_)) ++
      returnTypeChanges.map(exclude[IncompatibleResultTypeProblem](_)) ++
      otherProblems
  )

  private def newMethods = Seq(
    "org.scalacheck.commands.Commands.canCreateNewSut"
  )

  private def removedPrivateMethods = Seq(
  )

  private def removedPrivateClasses = Seq(
    "org.scalacheck.Platform$EnableReflectiveInstantiation"
  )

  private def methodTypeChanges = Seq(
    "org.scalacheck.Gen.oneOf",
    "org.scalacheck.Gen.sequence",
    "org.scalacheck.commands.Commands.canCreateNewSut",
    "org.scalacheck.util.Buildable.fromIterable"
  )

  private def returnTypeChanges = Seq(
    "org.scalacheck.Properties.properties",
    "org.scalacheck.Test.checkProperties"
  )

  private def otherProblems = Seq(
  )

}
