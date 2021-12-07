#!/bin/bash
set -evu

sbt_cmd=(sbt ++$TRAVIS_SCALA_VERSION)

if [[ "$PLATFORM" == "coreJS" ]]; then
  TESTS=100
else
  TESTS=1000
fi

for t in clean compile "testOnly * -- -s $TESTS -w $WORKERS" mimaReportBinaryIssues package doc; do
  sbt_cmd+=("$PLATFORM/$t")
done

echo "Running sbt: ${sbt_cmd[@]}"

"${sbt_cmd[@]}"
