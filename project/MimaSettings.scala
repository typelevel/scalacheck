object MimaSettings {

  import sbt._
  import com.typesafe.tools.mima
  import mima.core._
  import ProblemFilters.exclude
  import mima.plugin.MimaKeys.{binaryIssueFilters, previousArtifact}
  import mima.plugin.MimaPlugin.mimaDefaultSettings

  lazy val settings = mimaDefaultSettings ++ Seq(
    previousArtifact := Some("org.scalacheck" % "scalacheck_2.11" % "1.12.0"),
    binaryIssueFilters :=
      removedPrivateMethods.map(exclude[MissingMethodProblem](_)) ++
      newMethods.map(exclude[MissingMethodProblem](_)) ++
      removedPrivateClasses.map(exclude[MissingClassProblem](_)) ++
      otherProblems
  )

  private def newMethods = Seq(
    "org.scalacheck.Gen.collect"
  )

  private def removedPrivateMethods = Seq(
    "org.scalacheck.Arbitrary.org$scalacheck$Arbitrary$$chooseBigInt$1",
    "org.scalacheck.Arbitrary.org$scalacheck$Arbitrary$$chooseReallyBigInt$1",
    "org.scalacheck.Test.org$scalacheck$Test$$mergeResults$1"
  )

  private def removedPrivateClasses = Seq(
    "org.scalacheck.Test$Parameters$cp$",
    "org.scalacheck.Test$Parameters$cp"
  )

  private def otherProblems = Seq(
    // The ScalaCheckFramework class has changed in incompatible ways,
    // but hopefully this isn't a problem in practice
    exclude[FinalClassProblem]("org.scalacheck.ScalaCheckFramework"),
    exclude[MissingMethodProblem]("org.scalacheck.ScalaCheckFramework.tests"),
    exclude[MissingMethodProblem]("org.scalacheck.ScalaCheckFramework.testRunner"),
    exclude[MissingMethodProblem]("org.scalacheck.ScalaCheckFramework.testRunner"),

    // Properties.include has been changed (overloaded method instead of default
    // parameter) but hopefully it doesn't break code
    exclude[MissingMethodProblem]("org.scalacheck.Properties.include$default$2"),

    // Changes to private classes/methods

    exclude[IncompatibleResultTypeProblem]("org.scalacheck.Test#Parameters.org$scalacheck$Test$Parameters$$cp"),
    exclude[MissingMethodProblem]("org.scalacheck.Test#Parameters.org$scalacheck$Test$Parameters$$cp"),
    exclude[MissingMethodProblem]("org.scalacheck.Test#Parameters.org$scalacheck$Test$Parameters$_setter_$org$scalacheck$Test$Parameters$$cp_="),
    exclude[MissingMethodProblem]("org.scalacheck.Test#Parameters.TestParams"),

    exclude[MissingClassProblem]("org.scalacheck.util.CmdLineParser$OptVal"),
    exclude[MissingClassProblem]("org.scalacheck.util.CmdLineParser$OptVal$"),
    exclude[MissingMethodProblem]("org.scalacheck.util.CmdLineParser#OptMap.update"),
    exclude[MissingMethodProblem]("org.scalacheck.util.CmdLineParser#OptMap.this"),
    exclude[MissingTypesProblem]("org.scalacheck.util.CmdLineParser"),
    exclude[MissingMethodProblem]("org.scalacheck.util.CmdLineParser.org$scalacheck$util$CmdLineParser$$strVal"),
    exclude[MissingMethodProblem]("org.scalacheck.util.CmdLineParser.org$scalacheck$util$CmdLineParser$_setter_$org$scalacheck$util$CmdLineParser$$floatVal_="),
    exclude[MissingMethodProblem]("org.scalacheck.util.CmdLineParser.org$scalacheck$util$CmdLineParser$_setter_$options_="),
    exclude[MissingMethodProblem]("org.scalacheck.util.CmdLineParser.org$scalacheck$util$CmdLineParser$$optVal"),
    exclude[MissingMethodProblem]("org.scalacheck.util.CmdLineParser.org$scalacheck$util$CmdLineParser$_setter_$org$scalacheck$util$CmdLineParser$$strVal_="),
    exclude[MissingMethodProblem]("org.scalacheck.util.CmdLineParser.options"),
    exclude[MissingMethodProblem]("org.scalacheck.util.CmdLineParser.org$scalacheck$util$CmdLineParser$$OptVal"),
    exclude[MissingMethodProblem]("org.scalacheck.util.CmdLineParser.org$scalacheck$util$CmdLineParser$$opt"),
    exclude[MissingMethodProblem]("org.scalacheck.util.CmdLineParser.org$scalacheck$util$CmdLineParser$_setter_$org$scalacheck$util$CmdLineParser$$intVal_="),
    exclude[MissingMethodProblem]("org.scalacheck.util.CmdLineParser.parseArgs"),
    exclude[MissingMethodProblem]("org.scalacheck.util.CmdLineParser.org$scalacheck$util$CmdLineParser$$intVal"),
    exclude[MissingMethodProblem]("org.scalacheck.util.CmdLineParser.org$scalacheck$util$CmdLineParser$$floatVal"),
    exclude[MissingMethodProblem]("org.scalacheck.util.CmdLineParser.org$scalacheck$util$CmdLineParser$_setter_$org$scalacheck$util$CmdLineParser$$opt_="),
    exclude[MissingMethodProblem]("org.scalacheck.util.CmdLineParser.org$scalacheck$util$CmdLineParser$_setter_$org$scalacheck$util$CmdLineParser$$optVal_="),
    exclude[MissingMethodProblem]("org.scalacheck.util.CmdLineParser.parseArgs"),
    exclude[MissingMethodProblem]("org.scalacheck.util.CmdLineParser.OptMap"),
    exclude[MissingClassProblem]("org.scalacheck.util.CmdLineParser$ArgsReader")
  )

}
