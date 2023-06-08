/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

import sbt.*
import de.heikoseeberger.sbtheader.HeaderPlugin

object CustomHeaderPlugin extends AutoPlugin {
  import HeaderPlugin.autoImport.*

  override def trigger = allRequirements
  override def requires = HeaderPlugin

  override def projectSettings = Seq(
    headerLicense := Some(HeaderLicense.Custom(licenseTest))
  )

  private[this] final val licenseTest =
    """|ScalaCheck
       |Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
       |http://www.scalacheck.org
       |
       |This software is released under the terms of the Revised BSD License.
       |There is NO WARRANTY. See the file LICENSE for the full text.
       |""".stripMargin
}
