/*-------------------------------------------------------------------------*\
**  ScalaCheck                                                             **
**  Copyright (c) 2007-2008 Rickard Nilsson. All rights reserved.          **
**  http://www.scalacheck.org                                              **
**                                                                         **
**  This software is released under the terms of the Revised BSD License.  **
**  There is NO WARRANTY. See the file LICENSE for the full text.          **
\*-------------------------------------------------------------------------*/

package org.scalacheck.util

import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.Reader
import scala.util.parsing.input.Position
import scala.collection.Set

abstract sealed class Opt[+T](val default: T, val names: String*)
abstract class Flag(ns: String*) extends Opt[Unit]((), ns: _*)
abstract class IntOpt(d: Int, ns: String*) extends Opt[Int](d, ns: _*)
abstract class StrOpt(d: String, ns: String*) extends Opt[String](d, ns: _*)
 
class OptMap {
  private val opts = new collection.mutable.HashMap[Opt[_], Any]
  def apply(flag: Flag): Boolean = opts.contains(flag)
  def apply[T](opt: Opt[T]): T = opts.get(opt) match {
    case None => opt.default
    case Some(v: T) => v
  }
  def update[T](opt: Opt[T], optVal: T) = opts.update(opt, optVal)
}

class CmdLineParser(opts: Set[Opt[_]]) extends Parsers {

  type Elem = String

  private class ArgsReader(args: Array[String], i: Int) extends Reader[String] {
    val pos = new Position {
      val column = args.subArray(0,i).foldLeft(0)(_ + _.length)
      val line = 1
      val lineContents = args.mkString(" ")
    }
    val atEnd = i >= args.length
    def first = if(atEnd) "" else args(i)
    def rest = if(atEnd) this else new ArgsReader(args, i+1)
  }

  private val optName: Parser[String] = accept("option name", {
    case s if s.length > 0 && s.charAt(0) == '-' => s.drop(1)
  })

  private val strVal: Parser[String] = accept("string", {case s => s})

  private val intVal: Parser[Int] = accept("integer", {
    case s if s.forall(_.isDigit) => s.toInt
  })

  private case class OptVal[T](o: Opt[T], v: T)

  private val opt: Parser[OptVal[_]] = optName into { n =>
    opts.find(_.names.contains(n)) match {
      case None => failure("There is no option named " + n)
      case Some(o: Flag) => success(OptVal(o, ()))
      case Some(o: IntOpt) => intVal ^^ (v => OptVal(o, v))
      case Some(o: StrOpt) => strVal ^^ (v => OptVal(o, v))
    }
  }

  private val options: Parser[OptMap] = rep(opt) ^^ { xs =>
    val map = new OptMap
    xs.foreach { case OptVal(o,v) => map(o) = v }
    map
  }

  def parseArgs(args: Array[String]) = phrase(options)(new ArgsReader(args, 0))

}
