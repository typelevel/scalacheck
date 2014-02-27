package org.scalacheck

import org.scalacheck.Test.Parameters
import org.scalacheck.util.CmdLineParser

/**
 * Created with IntelliJ IDEA.
 * Author: Edmondo Porcu
 * Date: 28/02/14
 * Time: 10:18
 *
 */

trait ExtendedCommandLineRunner extends Properties {



  import Test.cmdLineParser.{Success, NoSuccess}


  def customRun(args: Array[String]): Int = {
    val maybeParams = Test.cmdLineParser.parseParams(args) map extendParams
    maybeParams match {
      case Success(params, _) =>
        if (Test.check(params, this).passed) 0
        else 1
      case e: NoSuccess =>
        println("Incorrect options:"+"\n"+e+"\n")
        Test.cmdLineParser.printHelp
        -1
    }
  }

  def extendParams(params:Parameters):Parameters


}
