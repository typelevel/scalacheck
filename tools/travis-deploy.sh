#!/bin/bash
set -evu

sbt_cmd=(sbt ++$TRAVIS_SCALA_VERSION)

if [[ "$DEPLOY" == "true" && "$TRAVIS_BRANCH" == "master" ]]; then
  "${sbt_cmd[@]}" "$PLATFORM/publish"
fi
