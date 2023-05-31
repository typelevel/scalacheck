/*
 * ScalaCheck                                                           
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.        
 * http://www.scalacheck.org                                            
 *                                                                      
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.        
 */

package org.scalacheck.examples

import org.scalacheck.Prop.{forAll, propBoolean}

object MathSpec extends org.scalacheck.Properties("Math") {
  property("sqrt") = forAll { (n: Int) =>
    (n >= 0) ==> {
      val m = math.sqrt(n.toDouble)
      math.round(m*m) == n
    }   
  }
}
