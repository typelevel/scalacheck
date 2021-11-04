---
title:  Download ScalaCheck
author: Rickard Nilsson
---

# Download ScalaCheck \$currentVer\$

## ScalaCheck \$currentVer\$ builds
Make sure you pick a build that matches the version of the Scala compiler
you're using. Scala versions other than the ones listed below is not supported
by ScalaCheck \$currentVer\$.

<table>
<tbody>
\$for(scalaVersions)\$
<tr class="odd">
<td align="left">[scalacheck\_\$scalaVersion\$-\$currentVer\$.jar](/files/scalacheck\_\$scalaVersion\$-\$currentVer\$.jar)</td>
<td align="left">(Scala \$scalaVersion\$)</td>
</tr>
\$endfor\$
</tbody>
</table>

## Sources and API documentation

|                                            |       |       |       |   |
|:-------------------------------------------|:------|:------|:------|:--|
| ScalaCheck \$currentVer\$ sources |[.jar](/files/scalacheck_\$scalaVer\$-\$currentVer\$-sources.jar)|[.zip](/files/scalacheck_\$scalaVer\$-\$currentVer\$-sources.zip)|[.tar.gz](/files/scalacheck_\$scalaVer\$-\$currentVer\$-sources.tar.gz)|[browse](\$repoUrl\$/tree/\$currentVer\$)|
| ScalaCheck \$currentVer\$ API docs|[.jar](/files/scalacheck_\$scalaVer\$-\$currentVer\$-javadoc.jar)|[.zip](/files/scalacheck_\$scalaVer\$-\$currentVer\$-javadoc.zip)|[.tar.gz](/files/scalacheck_\$scalaVer\$-\$currentVer\$-javadoc.tar.gz)|[browse](/files/scalacheck_\$scalaVer\$-\$currentVer\$-api/index.html)|

## sbt

```scala
libraryDependencies += "org.scalacheck" %% "scalacheck" % "$currentVer$" % "test"
```

## Other resources
* [ScalaCheck \$currentVer\$ release notes](\$repoUrl\$/tree/\$currentVer\$/RELEASE)
* [All ScalaCheck releases](/releases.html)

## License
ScalaCheck is released under the [3-clause BSD
license](\$repoUrl\$/tree/\$currentVer\$/LICENSE)
