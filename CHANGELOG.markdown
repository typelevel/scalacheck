# ScalaCheck CHANGELOG

## 1.14.1 (2019-09-18)

* Binary compatible with 1.14.0 version of ScalaCheck.

### Changed

* Deprecate ambiguous implicit `Prop.BooleanOperators` to prepare for
  Dotty compilation
  [#498](https://github.com/typelevel/scalacheck/pull/498)

* Show seed when failing test
  [#400](https://github.com/typelevel/scalacheck/issues/400)

* Underlying implementation of `Arbitrary[Option[T]]` is now
  equivalent to `Gen[Option[T]]`
  [#401](https://github.com/typelevel/scalacheck/issues/401)

* Update Scala.js 1.x to 1.0.0-M8, previously was 1.0.0-M3

* The phrases "EPFL" and "copyright owner" in the license were changed
  to "copyright holder"
  [#461](https://github.com/typelevel/scalacheck/pull/461)

### Fixed

* Fix overridding parameters with `overrideParameters` in `Properties`
  [#289](https://github.com/typelevel/scalacheck/issues/289),
  [#360](https://github.com/typelevel/scalacheck/issues/360)

* Fix shrinking of commands in 2.12
  [#468](https://github.com/typelevel/scalacheck/pull/468)

* Fix `Gen.posNum` and `Gen.negNum` so it gives real numbers less than
  one [#451](https://github.com/typelevel/scalacheck/pull/451)

* Fix independence of properties with `Prop.&&` with `viewSeed`
  enabled by sliding the seed in `Prop.flatMap`
  [#531](https://github.com/typelevel/scalacheck/pull/531)

* Fix stackoverflow of `Pretty.break`
  [#476](https://github.com/typelevel/scalacheck/pull/476)

* Fix error handling of command-line parsing
  [#522](https://github.com/typelevel/scalacheck/pull/522)

* Artifact for Scala.js 1.0.0-M8 for Scala 2.12 was
  defective [#496](https://github.com/typelevel/scalacheck/issues/496)

* Improve error messages for invalid command-line parameters
  [#497](https://github.com/typelevel/scalacheck/pull/497)

* Changes to support compilation on Scala 2.13
  [#410](https://github.com/typelevel/scalacheck/issues/410),
  [#480](https://github.com/typelevel/scalacheck/issues/480)

* Small fixes to source code for the Dotty compiler
  [#423](https://github.com/typelevel/scalacheck/pull/423)

* Fix compilation error in test runner for Java 11
  [#406](https://github.com/typelevel/scalacheck/issues/406)

* Fix compilation error in pretty-printer for Java 11 and later
  [#430](https://github.com/typelevel/scalacheck/pull/430)

* Fix deprecation in ScalaCheck's internal tokenizer for Java 11 and
  later [#433](https://github.com/typelevel/scalacheck/pull/433)

* Various corrections and improvements to the api docs
  [#415](https://github.com/typelevel/scalacheck/pull/415),
  [#417](https://github.com/typelevel/scalacheck/pull/417),
  [#420](https://github.com/typelevel/scalacheck/pull/420),
  [#428](https://github.com/typelevel/scalacheck/pull/428),
  [#467](https://github.com/typelevel/scalacheck/pull/467),
  [#524](https://github.com/typelevel/scalacheck/pull/524)

### Added

* Add new command-line option `-disableLegacyShrinking`
  [#522](https://github.com/typelevel/scalacheck/pull/522)

* Add new command-line option `-initialSeed`
  [#522](https://github.com/typelevel/scalacheck/pull/522)

* Add new methods for disabling and enabling shrinking to
  `Test.Parameters`
  [#522](https://github.com/typelevel/scalacheck/pull/522)

* Add support for `scala.util.Either` in `Gen`
  [#409](https://github.com/typelevel/scalacheck/pull/409)

* Add new overloaded version of `Gen.oneOf` that takes wider type of
  `Iterable` [#438](https://github.com/typelevel/scalacheck/pull/438)

* Add hexadecimal character and string generators `Gen.hexChar` and
  `Gen.hexStr`
  [#470](https://github.com/typelevel/scalacheck/pull/470)

### Administrivia

* The GitHub repository has been moved from Rickard Nilsson's private
  account to a repository under the Typelevel organization.

* ScalaCheck has adopted the Scala code of conduct.

* Erik Osheim accepted greater responsibility for ScalaCheck's
  maintenance and development.

* Aaron S. Hawley was added as a maintainer.

### Contributors

This release was made possible by contributions from the following people:

* Cody Allen
* Philippus Baalman
* Gio Borje
* Ryan Brewster
* Sébastien Doeraene
* Bruno Hass
* Aaron S. Hawley
* Magnolia K.
* Guillaume Martres
* Rickard Nilsson
* Erik Osheim
* Allan Renucci
* Nicolas Rinaudo
* Lukas Rytz
* Thomas Smith
* David Strawn
* Seth Tisue
* Kenji Yoshida

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

* Add support for `scala.collection.immutable.LazyList` in Scala 2.13,
  including `Gen.infiniteLazyList` and `Arbitrary[LazyList[T]]`
  (https://github.com/typelevel/scalacheck/issues/410)

* Support for filtering properties in the test runner
  (https://github.com/typelevel/scalacheck/pull/267).

* Support for setting the report column width used by the test runner.
