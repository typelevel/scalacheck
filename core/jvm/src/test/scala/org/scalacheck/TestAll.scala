/*
 * ScalaCheck                                                           
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.        
 * http://www.scalacheck.org                                            
 *                                                                      
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.        
 */

package org.scalacheck

object TestAll {
  object ScalaCheckSpecification extends Properties("ScalaCheck") {
    include(ArbitrarySpecification)
    include(GenSpecification)
    include(PropSpecification)
    include(TestSpecification)
    include(commands.CommandsSpecification)
  }

  def main(args: Array[String]) = ScalaCheckSpecification.main(args)
}
