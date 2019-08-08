# ScalaCheck CHANGELOG

## 1.14.0 (2018-04-22)

### Changed

* Binary incompatible with earlier versions of ScalaCheck. Make sure that any
  other test frameworks you're using are binary compatible with this release of
  ScalaCheck.

* The source code license was changed to the unmodified 3-clause BSD license.
  Previously, a slightly reworded 3-clause BSD license was used.

### Fixed

* Deadlock in test runner (https://github.com/typelevel/scalacheck/issues/290).

* Distribution issues in `Gen.pick`.

* Infinity issues in `Gen.choose`.

* Issues with test reporting when running sbt in forked mode.

### Added

* An `initialSeed` test parameter that can be set to make property
  evaluation deterministic. If the same seed is used, the generated test cases
  will be the same. By default, this initial seed is randomized (like in
  previous versions of ScalaCheck).

* A `filterNot()` method on generators.

* New generator combinator: `atLeastOne()`.

* New generators and `Arbitrary` instances for various types.

* Support for filtering properties in the test runner
  (https://github.com/typelevel/scalacheck/pull/267).

* Support for setting the report column width used by the test runner.
