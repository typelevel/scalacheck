#!/bin/bash
set -evu

sbt_cmd=(sbt ++$TRAVIS_SCALA_VERSION)

if [[ "$PLATFORM" == "js" ]]; then
  sbt_cmd+=("set scalaJSStage in Global := FastOptStage")
fi

if [[ "$SBT_PARALLEL" == "no" ]]; then
  sbt_cmd+=("set parallelExecution in ThisBuild := false")
fi

for t in clean update compile "testOnly * -- -s $TESTS -w $WORKERS" mimaReportBinaryIssues doc; do
  sbt_cmd+=("$PLATFORM/$t")
done

echo "Running sbt: ${sbt_cmd[@]}"

"${sbt_cmd[@]}"
