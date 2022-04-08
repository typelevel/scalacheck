# ScalaCheck CHANGELOG

## 1.16.0 (2022-04-07)

Scalacheck 1.16.0 is binary compatible with the 1.15.x and 1.14.x series. It is published for Scala 2.12, 2.13, and 3.1+ with Scala.js 1.8+ and Scala Native 0.4. This release is the first to support Scala 3 on the Native platform.

## User-facing PRs
- Adds Discord badge by @paualarco in https://github.com/typelevel/scalacheck/pull/814
- ScalaCheck follows semver since 1.14.x by @larsrh in https://github.com/typelevel/scalacheck/pull/793
- Improve docs for Arbitrary by @ashawley in https://github.com/typelevel/scalacheck/pull/806
- Update examples to 3.0 by @ashawley in https://github.com/typelevel/scalacheck/pull/813
- Add badge to README showing supported versions of Scala by @rtyley in https://github.com/typelevel/scalacheck/pull/819
- Fix `Arbitrary[Year]` instance max value by @dantb in https://github.com/typelevel/scalacheck/pull/825
- Check roundtrip in serialize tests by @ashawley in https://github.com/typelevel/scalacheck/pull/829
- Fix `Arbitrary[Char]` generates 0xFFFE (not a character in Unicode) by @kadzuya in https://github.com/typelevel/scalacheck/pull/835
- Ensure preconditions are satisified when shrinking Commands actions by @jonaskoelker in https://github.com/typelevel/scalacheck/pull/739
- Support `java.time` arbitraries on Scala.js & Native by @armanbilge in https://github.com/typelevel/scalacheck/pull/830
- Enable published snapshots by @armanbilge in https://github.com/typelevel/scalacheck/pull/877
- Onwards and upwards by @armanbilge in https://github.com/typelevel/scalacheck/pull/879

## 1.15.4 (2021-05-03)

* Binary compatible with 1.15.3 version of ScalaCheck.

### Added

* Docs for `Gen` by @ashawley 
  [#783](https://github.com/typelevel/scalacheck/pull/783)

* Add Arbitrary[Symbol] by @ashawley 
  * [#243](https://github.com/typelevel/scalacheck/issues/243)
  * [#785](https://github.com/typelevel/scalacheck/pull/785)

* Add Cogen Instance For UUID by @isomarcte 
  [#763](https://github.com/typelevel/scalacheck/pull/763)

### Changed

* Verbosity level for pretty-printing proofs by @ashawley 
  [#789](https://github.com/typelevel/scalacheck/pull/789)

* Hide ambiguous implicit buildableSeq by @ashawley 
  [#788](https://github.com/typelevel/scalacheck/pull/788)

* Cleanup of time by @ashawley 
  [#781](https://github.com/typelevel/scalacheck/pull/781)

* Initialize StringBuilder to suitable size by @martijnhoekstra 
  [#778](https://github.com/typelevel/scalacheck/pull/778)

* update ScalaCheck version in examples by @SethTisue 
  [#766](https://github.com/typelevel/scalacheck/pull/766)

### Fixed

* Fix tuple serialization by @ashawley and @yhylord 
  * [#672](https://github.com/typelevel/scalacheck/pull/672)
  * [#795](https://github.com/typelevel/scalacheck/pull/795)

* Fix overflow with Prop.all by @ashawley 
  * [#787](https://github.com/typelevel/scalacheck/pull/787)
  * [#748](https://github.com/typelevel/scalacheck/issues/748)

* Fix Off By One Error In JavaTimeChoose by @isomarcte 
  * [#762](https://github.com/typelevel/scalacheck/issues/762)
  * [#769](https://github.com/typelevel/scalacheck/pull/769)

* Fix typos by @bwignall 
  [#767](https://github.com/typelevel/scalacheck/pull/767)

## 1.15.3 (2021-02-16)

* Binary compatible with 1.15.2 version of ScalaCheck.

### Changed

* Update Scala.js to 1.5.0
  [#757](https://github.com/typelevel/scalacheck/pull/757)

* Support for Scala Native 0.4.0 by @larsrh 
  [#751](https://github.com/typelevel/scalacheck/pull/751)

* Add Cogen.domainOf by @nigredo-tori 
  [#756](https://github.com/typelevel/scalacheck/pull/756)

### Fixed

* Make printing of Commands failures more readable by @jonaskoelker 
  [#634](https://github.com/typelevel/scalacheck/issues/634)

## 1.15.2 (2020-12-07)

* Binary compatible with 1.15.1 version of ScalaCheck.

### Changed

* Avoid filtering in `nonEmptyBuildableOf`
  [#709](https://github.com/typelevel/scalacheck/pull/709)

* Drop support for Scala.js 0.6
  [#713](https://github.com/typelevel/scalacheck/pull/713)

* Add support for Scala.js on Scala 3
  [#713](https://github.com/typelevel/scalacheck/pull/712)

### Fixed

* Regression in `Gen.nonEmptyBuildableOf` and dependent methods
  * [#708](https://github.com/typelevel/scalacheck/issues/708)
  * [#714](https://github.com/typelevel/scalacheck/issues/714)

## 1.15.1 (2020-11-06)

* Binary compatible with 1.15.0 version of ScalaCheck.

### Changed

* No user-visible changes.

### Fixed

* Return types of `Gen.someOf` and `Gen.atLeastOne` corrected to
  preserve source compatibility with Scala 2.13 artifact for
  1.14.3 ScalaCheck.
  [#696](https://github.com/typelevel/scalacheck/issues/696)

* Fix breakage with version of `Gen.pick` that takes multiple
  arguments of `Gen[T]`.
  [#695](https://github.com/typelevel/scalacheck/pull/695)

### Added

* No added features.

## 1.15.0 (2020-10-31)

* Binary compatible with 1.14.3 version of ScalaCheck.

* Source incompatible type signatures of `Gen.atLeastOne` and
  `Gen.someOf` in Scala 2.13 artifact(s) of 1.14.3 version of
  ScalaCheck that will be fixed in 1.15.1.

### Changed

* Dropped support for Scala 2.10.x

* Remove implicit for `Prop.BooleanOperators` which was deprecated in
  1.14.1 in favor of `Prop.propBoolean`
  [#667](https://github.com/typelevel/scalacheck/pull/667)

* Added support for Dotty (currently 0.27)

* Various improvements to `Gen` and `Arbitrary`
  * [#603](https://github.com/typelevel/scalacheck/pull/603)
  * [#604](https://github.com/typelevel/scalacheck/pull/604)
  * [#605](https://github.com/typelevel/scalacheck/pull/605)
  * [#606](https://github.com/typelevel/scalacheck/pull/606)
  * [#607](https://github.com/typelevel/scalacheck/pull/607)
  * [#610](https://github.com/typelevel/scalacheck/pull/610)

* Improvements to `Char` and `String` generators
  * [#653](https://github.com/typelevel/scalacheck/pull/653)

* Single-argument `Prop.collect` is deprecated
  [#449](https://github.com/typelevel/scalacheck/pull/449)

### Fixed

* Prevent nesting properties
  [#677](https://github.com/typelevel/scalacheck/pull/677)

* Seed with four zeroes is not allowed
  [#674](https://github.com/typelevel/scalacheck/pull/674)

* Printing order of shrunk values
  [#635](https://github.com/typelevel/scalacheck/pull/635)

* Initial seed was reused
  [#651](https://github.com/typelevel/scalacheck/pull/651)

### Added

* `Choose[BigDecimal]`
  [#670](https://github.com/typelevel/scalacheck/pull/670)

* `Choose[BigInt]`
  [#636](https://github.com/typelevel/scalacheck/pull/636)

* Statistical distributions (e.g. Gaussian, Poisson)
  [#656](https://github.com/typelevel/scalacheck/pull/656)

* Shrinking with `LazyList` for Scala 2.13 compatibility
  [#626](https://github.com/typelevel/scalacheck/issues/626)
  [#627](https://github.com/typelevel/scalacheck/pull/627)

* `Shrink.suchThat`
  [#484](https://github.com/typelevel/scalacheck/pull/484)

* `Gen.recursive` for recursive generators
  [#616](https://github.com/typelevel/scalacheck/pull/616)
  [#639](https://github.com/typelevel/scalacheck/pull/639)

## 1.14.3 (2019-12-13)

### Changed

* Update Scala.js to 1.0.0-RC2, previously was 1.0.0-M8.

* Update Scala.js to 0.6.31, previously was 0.6.29.

### Fixed

* Ensure posNum and negNum always return values
  [#568](https://github.com/typelevel/scalacheck/issues/568)

## 1.14.2 (2019-09-25)

* Binary compatible with 1.14.1 version of ScalaCheck.

### Changed

* No user-visible changes.

### Fixed

* Tests are not being run at all on Scala.js
  [#557](https://github.com/typelevel/scalacheck/issues/557)

### Added

* No added features.

### Administrivia

* Further improvements to ScalaCheck release script

* Update build to sbt 1.3.2

* Update MiMa plugin to 0.6.1

* Update sbt-pgp to 2.0.0

* Update Scala.js example project

### Contributors

This release was made possible by contributions from the following people:

* Aaron S. Hawley
* Erik Osheim
* Scala Steward
* Kenji Yoshida

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
