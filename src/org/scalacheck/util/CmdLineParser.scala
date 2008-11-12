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

trait Opt[+T] {
  val default: T
  val names: Set[String]
  val help: String
}
trait Flag extends Opt[Unit]
trait IntOpt extends Opt[Int]
trait StrOpt extends Opt[String]

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
      val column = args.subArray(0,i).foldLeft(1)(_ + _.length + 1)
      val line = 1
      val lineContents = args.mkString(" ")
    }
    val atEnd = i >= args.length
    def first = if(atEnd) null else args(i)
    def rest = if(atEnd) this else new ArgsReader(args, i+1)
  }

  private def getOpt(s: String) = {
    if(s == null || s.length == 0 || s.charAt(0) != '-') None
    else opts.find(_.names.contains(s.drop(1)))
  }
  
  private val opt: Parser[Opt[Any]] = accept("option name", {
    case s if getOpt(s).isDefined => getOpt(s).get
  })

  private val strVal: Parser[String] = accept("string", {
    case s if s != null => s
  })

  private val intVal: Parser[Int] = accept("integer", {
    case s if s != null && s.length > 0 && s.forall(_.isDigit) => s.toInt
  })

  private case class OptVal[T](o: Opt[T], v: T)

  private val optVal: Parser[OptVal[Any]] = opt into {
    case o: Flag => success(OptVal(o, ()))
    case o: IntOpt => intVal ^^ (v => OptVal(o, v))
    case o: StrOpt => strVal ^^ (v => OptVal(o, v))
  }

  private val options: Parser[OptMap] = rep(optVal) ^^ { xs =>
    val map = new OptMap
    xs.foreach { case OptVal(o,v) => map(o) = v }
    map
  }

  def parseArgs(args: Array[String]) = phrase(options)(new ArgsReader(args, 0))

}
