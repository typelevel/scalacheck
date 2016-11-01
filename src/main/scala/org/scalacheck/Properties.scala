/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2015 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck

import language.reflectiveCalls

import util.ConsoleReporter

/** Represents a collection of properties, with convenient methods
 *  for checking all properties at once. This class is itself a property, which
 *  holds if and only if all of the contained properties hold.
 *  <p>Properties are added in the following way:</p>
 *
 *  {{{
 *  object MyProps extends Properties("MyProps") {
 *    property("myProp1") = forAll { (n:Int, m:Int) =>
 *      n+m == m+n
 *    }
 *  }
 *  }}}
 */
class Properties(val name: String) extends Prop {

  private val props = new scala.collection.mutable.ListBuffer[(String,Prop)]

  /** Returns one property which holds if and only if all of the
   *  properties in this property collection hold */
  private def oneProperty: Prop = Prop.all((properties map (_._2)):_*)

  /** Returns all properties of this collection in a list of name/property
   *  pairs.  */
  def properties: Seq[(String,Prop)] = props

  def apply(p: Gen.Parameters) = oneProperty(p)

  /** Convenience method that checks the properties with the given parameters
   *  and reports the result on the console. If you need to get the results
   *  from the test use the `check` methods in [[org.scalacheck.Test]]
   *  instead. */
  override def check(prms: Test.Parameters): Unit = Test.checkProperties(
    prms.withTestCallback(ConsoleReporter(1) chain prms.testCallback), this
  )

  /** Convenience method that checks the properties and reports the
   *  result on the console. If you need to get the results from the test use
   *  the `check` methods in [[org.scalacheck.Test]] instead. */
  override def check: Unit = check(Test.Parameters.default)

  /** The logic for main, separated out to make it easier to
   *  avoid System.exit calls.  Returns exit code.
   */
  override def mainRunner(args: Array[String]): Int = {
    Test.cmdLineParser.parseParams(args) match {
      case (params, Nil) =>
        val res = Test.checkProperties(params, this)
        val failed = res.filter(!_._2.passed).size
        failed
      case (_, os) =>
        println(s"Incorrect options: $os")
        Test.cmdLineParser.printHelp
        -1
    }
  }

  /** Adds all properties from another property collection to this one */
  def include(ps: Properties): Unit =
    include(ps, prefix = "")

  /** Adds all properties from another property collection to this one
   *  with a prefix this is prepended to each included property's name. */
  def include(ps: Properties, prefix: String): Unit =
    for((n,p) <- ps.properties) property(prefix + n) = p

  /** Used for specifying properties. Usage:
   *  {{{
   *  property("myProp") = ...
   *  }}}
   */
  class PropertySpecifier() {
    // TODO: Delete this in 1.14 -- kept for binary compat with 1.13.3 and prior
    protected def update(propName: String, p: Prop) = {
      props += ((name+"."+propName, p))
    }
    def update(propName: String, p: => Prop) = {
      props += ((name+"."+propName, Prop.delay(p)))
    }
  }

  lazy val property = new PropertySpecifier()
}
