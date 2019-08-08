package fix

import scalafix.v1._
import scala.meta._

class v1_14_1 extends SemanticRule("v1_14_1") {
  override def fix(implicit doc: SemanticDocument): Patch = {
    BooleanOperatorsRules(doc.tree).asPatch
  }
}

/** Replace deprecated BooleanOperators by propBoolean */
object BooleanOperatorsRules {
  val propObject    = SymbolMatcher.exact("org/scalacheck/Prop.")
  val deprecatedDef = SymbolMatcher.exact("org/scalacheck/Prop.BooleanOperators().")

  def apply(t: Tree)(implicit doc: SemanticDocument): List[Patch] = t.collect {
    case Term.Apply(fun, _) if deprecatedDef.matches(fun.symbol) =>
      fun.collect {
        case name @ Term.Name("BooleanOperators") =>
          Patch.replaceTree(name, "propBoolean")
      }.asPatch
    // Imports don't have symbols, but import prefixes do, so we match on the
    // enclosing object.
    case Importer(ref, importees) if propObject.matches(ref.symbol) =>
      importees.collect {
        case Importee.Name(name @ Name("BooleanOperators")) =>
          Patch.replaceTree(name, "propBoolean")
      }.asPatch
  }
}
