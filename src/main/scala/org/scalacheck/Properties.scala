/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2010 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*-------------------------------------------------------------------------*/

package org.scalacheck

/** Represents a collection of properties, with convenient methods
 *  for checking all properties at once. This class is itself a property, which
 *  holds if and only if all of the contained properties hold.
 *  <p>Properties are added in the following way:</p>
 *
 *  <p>
 *  <code>
 *  object MyProps extends Properties("MyProps") {
 *    property("myProp1") = forAll { (n:Int, m:Int) =&gt;
 *      n+m == m+n
 *    }
 *
 *    property("myProp2") = ((0/1) throws classOf[ArithmeticException])
 *  }
 */
class Properties(val name: String) extends Prop {

  private val props = new scala.collection.mutable.ListBuffer[(String,Prop)]

  /** Returns one property which holds if and only if all of the
   *  properties in this property collection hold */
  private def oneProperty: Prop = Prop.all((properties map (_._2)):_*)

  /** Returns all properties of this collection in a list of name/property
   *  pairs.  */
  def properties: Seq[(String,Prop)] = props

  def apply(p: Prop.Params) = oneProperty(p)

  override def check(prms: Test.Params): Unit = Test.checkProperties(prms, this)

  /** Adds all properties from another property collection to this one. */
  def include(ps: Properties) = for((n,p) <- ps.properties) property(n) = p 

  /** Used for specifying properties. Usage:
   *  <code>property("myProp") = ...</code> */
  class PropertySpecifier() {
    def update(propName: String, p: Prop) = props += ((name+"."+propName, p))
  }
  lazy val property = new PropertySpecifier()
}
