object MimaSettings {

  import sbt._
  import com.typesafe.tools.mima
  import mima.core.ProblemFilters.exclude
  import mima.plugin.MimaKeys.{binaryIssueFilters, previousArtifact}
  import mima.plugin.MimaPlugin.mimaDefaultSettings

  lazy val settings = mimaDefaultSettings ++ Seq(
    previousArtifact := Some("org.scalacheck" % "scalacheck_2.11" % "1.12.0"),
    binaryIssueFilters :=
      removedPrivateMethods.map(exclude[mima.core.MissingMethodProblem](_)) ++
      newMethods.map(exclude[mima.core.MissingMethodProblem](_)) ++
      removedPrivateClasses.map(exclude[mima.core.MissingClassProblem](_))
  )

  private def newMethods = Seq(
    "org.scalacheck.Gen.collect"
  )

  private def removedPrivateMethods = Seq(
    "org.scalacheck.Arbitrary.org$scalacheck$Arbitrary$$chooseBigInt$1",
    "org.scalacheck.Arbitrary.org$scalacheck$Arbitrary$$chooseReallyBigInt$1"
  )

  private def removedPrivateClasses = Seq(
    "org.scalacheck.Test$Parameters$cp$"
  )

}
