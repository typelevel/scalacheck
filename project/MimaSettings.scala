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
      removedPublicMethods.map(exclude[DirectMissingMethodProblem](_)) ++
      changedMethods.map(exclude[IncompatibleMethTypeProblem](_)) ++
      newMethods.map(exclude[ReversedMissingMethodProblem](_)) ++
      removedPrivateClasses.map(exclude[MissingClassProblem](_)) ++
      otherProblems
  )

  private def newMethods = Seq(
  )

  private def removedPrivateMethods = Seq(
  )

  private def removedPrivateClasses = Seq(
    "org.scalacheck.Platform$EnableReflectiveInstantiation"
  )

  private def otherProblems = Seq(
  )

  private def removedPublicMethods = Seq(
    "org.scalacheck.Prop.forAll"
  )

  private def changedMethods = Seq(
    "org.scalacheck.Prop.forAll"
  )

}
