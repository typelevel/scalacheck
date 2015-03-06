package org.scalacheck
class GenMacroImpls(val c: reflect.macros.blackbox.Context){
  import c.universe._

  def partialTree[T: c.WeakTypeTag]: Tree = treeHelper[T](true)

  def tree[T: c.WeakTypeTag]: Tree = treeHelper[T](false)

  /** Generates a list of all known classes and traits in an inheritance tree. Does not include subclasses of non-sealed ones. */
  private def linerizedInheritanceTree(symbol:Symbol): Seq[Symbol] = {
    def children = symbol.asClass.knownDirectSubclasses.flatMap(linerizedInheritanceTree)
    symbol match{
      case s if s.isModuleClass => Seq(s)
      case s if s.isClass => //  && s.asClass.isCaseClass
        Seq(s) ++ children
      case s =>
        c.abort(c.enclosingPosition, s"Can create generator for $s.")
    }
  }      

  private def caseObject(sym: Symbol) =
    sym.asInstanceOf[reflect.internal.Symbols#Symbol].sourceModule.asInstanceOf[Symbol]

  private val scalacheck = q"_root_.org.scalacheck"
  private val Gen       =  q"$scalacheck.Gen"
  private val Arbitrary =  q"$scalacheck.Arbitrary.apply"
  private def Arbitrary(T: Type) = tq"$scalacheck.Arbitrary[$T]"

  private def treeHelper[T: c.WeakTypeTag](allowOpenHierarchies: Boolean): Tree = {
      val T = weakTypeOf[T]
      val symbol = T.typeSymbol

      val classes = linerizedInheritanceTree(symbol)

      if(symbol.isClass && symbol.asClass.isAbstract && !symbol.asClass.isSealed)
        c.abort(c.enclosingPosition, s"$T is abstract but not sealed. No subclasses can be found.")

      val gens = classes.collect{
        case s if s.isModuleClass => q"$Gen.const(${caseObject(s)})"

        case s if !s.isClass =>
          c.abort(c.enclosingPosition, s"Can not create generator for non-class type $s.")

        case s: ClassSymbol if !allowOpenHierarchies && !s.isFinal && !s.isSealed =>
          c.abort(c.enclosingPosition, s"Can not create generator for non-sealed, non-final $s. Use partialTree if you cannot make all classes and traits sealed or finaled. Be aware that partialTree does not generate subclasses of non-sealed classes or traits.")

        case s: ClassSymbol if !s.isAbstract => 
          val paramLists = s.typeSignature.decls.collectFirst {
            case m: MethodSymbol if m.isPrimaryConstructor => m
          }.get.paramLists

          if(paramLists.flatten.size > 22){
            c.abort(c.enclosingPosition, s"Can not create generator for class with more than 22 paramters: $s")
          } else if(paramLists.flatten.size > 0){
            val identsAndTypes = paramLists.map( _.map( s => (TermName(c.freshName("v")), s.typeSignature) ) )
            val args = identsAndTypes.flatten.map{ case (n,t) => q"$n: $t" }
            val idents = identsAndTypes.map( _.map(_._1) )
            q"$Gen.resultOf((..$args) => new $s(...$idents))"
          } else {
            q"$Gen.wrap($Gen.const(new ${s}))" // wrap to generate different object instances
          }
      }

      if(gens.isEmpty)
        c.abort(c.enclosingPosition, s"No concrete classes found extending $T. (Only subclasses of sealed traits and classes can be found.)")

      val arb = TermName(c.freshName("arb"))

      // using lzy to allow circular recursion
      q"""{
        /** implicit to automatically wire recursive datastructure */
        implicit def $arb: ${Arbitrary(T)} = $Arbitrary($Gen.lzy($Gen.oneOf(..$gens)))
        $arb.arbitrary
      }
      """
  }
}
