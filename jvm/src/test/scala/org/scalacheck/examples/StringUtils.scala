/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2019 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck.example

import org.scalacheck._

object StringUtils extends Properties("Examples.StringUtils") {

  private object StringUtils {
    def truncate(s: String, n: Int) = {
      if(s.length <= n) s
      else s.substring(0, n) ++ "..."
    }

    def truncate2(s: String, n: Int) = {
      if(n < 0) ""
      else if(s.length <= n) s
      else s.substring(0, n) ++ "..."
    }

    def tokenize(s: String, delim: Char) = {
      val delimStr = Character.valueOf(delim).toString
      val st = new java.util.StringTokenizer(s, delimStr)
      val tokens = Array.ofDim[String](st.countTokens)
      var i = 0;
      while(st.hasMoreTokens) {
        tokens(i) = st.nextToken()
        i += 1;
      }
      tokens;
    }

    def contains(s: String, subString: String) = {
      s.indexOf(subString) != -1
    }
  }

  property("truncate") = Prop.forAll { (s: String, n: Int) =>
    lazy val t = StringUtils.truncate(s, n)
    if(n < 0)
      Prop.throws(classOf[StringIndexOutOfBoundsException]) { t }
    else 
      (s.length <= n && t == s) ||
      (s.length > n && t == s.take(n)+"...")
  }

  property("truncate.precond") = Prop.forAll { (s: String, n: Int) =>
    import Prop.propBoolean
    (n >= 0) ==> {
      val t = StringUtils.truncate(s, n)
      (s.length <= n && t == s) ||
      (s.length > n && t == s.take(n)+"...")
    }
  }

  property("truncate2") = Prop.forAll { (s: String, n: Int) =>
    val t = StringUtils.truncate2(s, n)
    if(n < 0)
      t == ""
    else
      (s.length <= n && t == s) ||
      (s.length > n && t == s.take(n)+"...")
  }

  //property("tokenize") = {
  //  import Prop.AnyOperators
  //  Prop.forAll(Gen.listOf(Gen.alphaStr), Gen.numChar) { (ts, d) =>
  //    val str = ts.mkString(d.toString)
  //    StringUtils.tokenize(str, d).toList ?= ts
  //  }
  //}

  property("contains") =
    Prop.forAll { (s1: String, s2: String, s3: String) =>
      StringUtils.contains(s1+s2+s3, s2)
    }
}
