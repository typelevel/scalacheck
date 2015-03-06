#!/bin/sh
cd "$(dirname "$0")"
scala codegen.scala Arbitrary > ../src/main/scala/org/scalacheck/ArbitraryArities.scala
scala codegen.scala Gen > ../src/main/scala/org/scalacheck/GenArities.scala
