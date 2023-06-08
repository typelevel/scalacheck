/*
 * ScalaCheck                                                           
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.        
 * http://www.scalacheck.org                                            
 *                                                                      
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.        
 */

package org.scalacheck.time

import java.time._
import org.scalacheck.Prop._
import org.scalacheck.Shrink._
import org.scalacheck._

object ShrinkSpecification extends Properties ("time.Shrink"){
  property("shrink[Duration]") = forAll { (n: Duration) =>
    !shrink(n).contains(n)
  }

  property("shrink[Period]") = forAll { (n: Period) =>
    !shrink(n).contains(n)
  }
}
