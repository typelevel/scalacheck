# ScalaCheck CHANGELOG

## 1.14.0 (UNRELEASED)

### Changed

* Binary incompatible with earlier versions of ScalaCheck. Make sure that any
  other test frameworks you're using are binary compatible with this release of
  ScalaCheck.

* The source code license was changed to the unmodified 3-clause BSD license.
  Previously, a slightly reworded 3-clause BSD license was used.

### Fixed

* Deadlock in test runner (https://github.com/rickynils/scalacheck/issues/290)

### Added

* Add an `initialSeed` test parameter that can be set to make property
  evaluation deterministic. If the same seed is used, the generated test cases
  will be the same. By default, this initial seed is randomized (like in
  previous versions of ScalaCheck).

* New generators and `Arbitrary` instances added for various types.

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
