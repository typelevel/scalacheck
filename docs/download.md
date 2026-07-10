# Download

ScalaCheck is available for the JVM, Scala.js, and Scala Native.

## Latest stable

Use the latest stable release unless you need to stay on an older line for compatibility reasons.

### sbt

#### JVM

```scala
libraryDependencies += "org.scalacheck" %% "scalacheck" % "@VERSION@" % Test
```

#### Scala.js / Scala Native

```
libraryDependencies += "org.scalacheck" %%% "scalacheck" % "@VERSION@" % Test
```

### Scala CLI

```scala```
//> using dep "org.scalacheck::scalacheck:@VERSION@"
```

### Mill

```scala
ivy"org.scalacheck::scalacheck:@VERSION@"
```

## Release lines

| Line   | Representative Release | Date       | Scala                | Scala.js | Scala Native | Notes                          |
|--------|------------------------|------------|----------------------|----------|--------------|--------------------------------|
| 1.19.x | 1.19.0                | 2025-09-06 | 2.12, 2.13, 3.x      | 1.x      | 0.5          | Latest stable                  |
| 1.18.x | 1.18.1                | 2024-09-15 | 2.12, 2.13, 3.x      | 1.x      | 0.5          | Maintenance release            |
| 1.17.x | 1.17.1                | 2024-04-16 | 2.12, 2.13, 3.x      | 1.x      | 0.4          | Stable historical line         |
| 1.16.x | 1.16.0                | 2022-04-07 | 2.12, 2.13, 3.x      | 1.x      | 0.4          | Adds support for java.time     |
| 1.15.x | 1.15.4                | 2021-05-03 | 2.12, 2.13, 3.x*     | 1.x      | 0.4          | Bug-fix release                |

* Scala 3 artifacts for ScalaCheck 1.15.4 and 1.15.3 were published on 2021-05-14.

### Use the representative release version

If you want something other than the latest stable release, select the representative release version of your choice. For example, for version `1.17.1`, apply the following in your build tool:

```scala
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.17.1" % Test
```

## Notes

- The JVM dependency uses `%%` to fill in the Scala version.
- Scala.js and Scala Native use `%%%` to fill in the Scala version.
- ScalaCheck is normally added with the `Test` scope.

## Related pages

- [API](api.md)
- [Sources](sources.md)
