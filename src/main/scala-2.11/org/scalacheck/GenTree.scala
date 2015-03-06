package org.scalacheck
import scala.language.experimental.macros
trait GenTree{	
  /** Generator that produces instances of the given class and its subclasses
      using their primary constructors, skipping traits and abstract classes.

      NOTE: Expects all involed traits and classes to be sealed or final.
  */
  def tree[T]: Gen[T] = macro GenMacroImpls.tree[T]

  /** Generator that produces instances of the given class and its subclasses
      using their primary constructors, skipping traits and abstract classes.

      NOTE: Does not see subclasses of non-sealed classes and traits.
  */
  def partialTree[T]: Gen[T] = macro GenMacroImpls.partialTree[T]
}
