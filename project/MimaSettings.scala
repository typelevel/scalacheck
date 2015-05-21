object MimaSettings {

  import sbt._
  import com.typesafe.tools.mima
  import mima.core._
  import ProblemFilters.exclude
  import mima.plugin.MimaKeys.{binaryIssueFilters, previousArtifact}
  import mima.plugin.MimaPlugin.mimaDefaultSettings

  lazy val settings = mimaDefaultSettings ++ Seq(
    previousArtifact := Some("org.scalacheck" % "scalacheck_2.11" % "1.12.3"),
    binaryIssueFilters :=
      removedPrivateMethods.map(exclude[MissingMethodProblem](_)) ++
      newMethods.map(exclude[MissingMethodProblem](_)) ++
      removedPrivateClasses.map(exclude[MissingClassProblem](_)) ++
      otherProblems
  )

  private def newMethods = Seq(
    "org.scalacheck.Test#Parameters.toString"
  )

  private def removedPrivateMethods = Seq(
    // TestParams was private[scalacheck] in 1.12.3, which was a mistake
    "org.scalacheck.Test#Parameters.org$scalacheck$Test$Parameters$_setter_$org$scalacheck$Test$Parameters$$cp_=",
    "org.scalacheck.Test#Parameters.TestParams",
    "org.scalacheck.Test#Parameters.org$scalacheck$Test$Parameters$$TestParams",
    "org.scalacheck.Test#Parameters#TestParams.org$scalacheck$Test$Parameters$_setter_$org$scalacheck$Test$Parameters$$cp_=",
    "org.scalacheck.Test#Parameters#TestParams.TestParams"
  )

  private def removedPrivateClasses = Seq(
  )

  private def otherProblems = Seq(
  )

}
