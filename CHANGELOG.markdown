# ScalaCheck CHANGELOG

## 1.14.0 (UNRELEASED)

### Changed

* Binary incompatible with earlier versions of ScalaCheck. Make sure that any
  other test frameworks you're using are binary compatible with this release of
  ScalaCheck.

### Added

* Support for filtering properties in the test runner
  (https://github.com/rickynils/scalacheck/pull/267).

  Example usage with sbt:

  ```
  > testOnly -- -f .*choose.*
  [info] + Gen.choose-long: OK, passed 100 tests.
  [info] + Gen.choose-int: OK, passed 100 tests.
  [info] + Gen.choose finite duration values are within range: OK, passed 100 tests.
  [info] + Gen.choose-xmap: OK, passed 100 tests.
  [info] + Gen.choose-large-double: OK, passed 100 tests.
  [info] + Gen.choose-double: OK, passed 100 tests.
  [info] + Serializability.Gen[choose] serializability: OK, proved property.
  [info] Passed: Total 7, Failed 0, Errors 0, Passed 7
  [success] Total time: 1 s, completed Nov 18, 2016 2:10:57 PM
  ```
