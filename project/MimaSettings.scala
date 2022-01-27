object MimaSettings {

  import sbt._
  import com.typesafe.tools.mima
  import mima.core._
  import ProblemFilters.exclude
  import mima.plugin.MimaKeys.mimaBinaryIssueFilters

  lazy val settings = Seq(
    mimaBinaryIssueFilters :=
      removedPrivateMethods.map(exclude[DirectMissingMethodProblem](_)) ++
      newMethods.map(exclude[ReversedMissingMethodProblem](_)) ++
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
    "org.scalacheck.Shrink.shrinkTuple2",
    "org.scalacheck.Shrink.shrinkTuple3",
    "org.scalacheck.Shrink.shrinkTuple4",
    "org.scalacheck.Shrink.shrinkTuple5",
    "org.scalacheck.Shrink.shrinkTuple6",
    "org.scalacheck.Shrink.shrinkTuple7",
    "org.scalacheck.Shrink.shrinkTuple8",
    "org.scalacheck.Shrink.shrinkTuple9"
  ).map(m => ProblemFilters.exclude[IncompatibleSignatureProblem](m))

}
