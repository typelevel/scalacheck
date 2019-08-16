#!/bin/bash
set -evu

sbt_cmd=(sbt ++$TRAVIS_SCALA_VERSION)

if [[ "$PLATFORM" == "js" ]]; then
  TESTS=100
else
  TESTS=1000
fi

sbt_cmd+=("set parallelExecution in ThisBuild := ${SBT_PARALLEL:-true}")

for t in clean compile "testOnly * -- -s $TESTS -w ${WORKERS:-1}" mimaReportBinaryIssues doc; do
  sbt_cmd+=("${PLATFORM:-jvm}/$t")
done

echo "Running sbt: ${sbt_cmd[@]}"

"${sbt_cmd[@]}"
