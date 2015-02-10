/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2015 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*------------------------------------------------------------------------ */

package org.scalacheck.util

import scala.collection.Set
import org.scalacheck.Test

private[scalacheck] trait CmdLineParser {

  trait Opt[+T] {
    val default: T
    val names: Set[String]
    val help: String
  }
  trait Flag extends Opt[Unit]
  trait IntOpt extends Opt[Int]
  trait FloatOpt extends Opt[Float]
  trait StrOpt extends Opt[String]

  class OptMap(private val opts: Map[Opt[_],Any] = Map.empty) {
    def apply(flag: Flag): Boolean = opts.contains(flag)
    def apply[T](opt: Opt[T]): T = opts.get(opt) match {
      case None => opt.default
      case Some(v) => v.asInstanceOf[T]
    }
    def set[T](o: (Opt[T], T)) = new OptMap(opts + o)
  }

  val opts: Set[Opt[_]]

  private def getOpt(s: String) = {
    if(s == null || s.length == 0 || s.charAt(0) != '-') None
    else opts.find(_.names.contains(s.drop(1)))
  }

  private def getStr(s: String) = Some(s)

  private def getInt(s: String) =
    if (s != null && s.length > 0 && s.forall(_.isDigit)) Some(s.toInt)
    else None

  private def getFloat(s: String) =
    if (s != null && s.matches("[0987654321]+\\.?[0987654321]*")) Some(s.toFloat)
    else None

  def printHelp = {
    println("Available options:")
    opts.foreach { opt =>
      println("  " + opt.names.map("-"+_).mkString(", ") + ": " + opt.help)
    }
  }

  def parseArgs[T](args: Array[String])(f: OptMap => T) = {
    def parseOptVal[U](o: Opt[U], f: String => Option[U], as: List[String]): Option[OptMap] = for {
      v <- as.headOption.flatMap(f)
      om <- parse(as.drop(1))
    } yield om.set((o,v))

    def parse(as: List[String]): Option[OptMap] = as match {
      case Nil => Some(new OptMap)
      case a::as => getOpt(a) flatMap {
        case o: Flag => parse(as).map(_.set((o,())))
        case o: IntOpt => parseOptVal(o, getInt, as)
        case o: FloatOpt => parseOptVal(o, getFloat, as)
        case o: StrOpt => parseOptVal(o, getStr, as)
      }
    }

    parse(args.toList).map(f)
  }
}
