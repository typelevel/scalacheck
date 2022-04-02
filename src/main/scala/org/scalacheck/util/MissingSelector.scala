package org.scalacheck.util

// Modified red-black order statistic tree to select missing elements.
private[scalacheck] sealed trait MissingSelector {
  /** Select (and add) the i-th non-negative integer not present.
   * @param i
   * @return The i-th non negative integer not present in the original MissingSelector,
   *         and a new MissingSelector containing this integer.
   */
  def selectAndAdd(i: Int): (Int, MissingSelector)
  protected def selAndAdd(i: Int): (Int, MissingSelector.Inner)
  def size: Int
  def toList(others: List[Int] = List.empty[Int]): List[Int]
}

private[scalacheck] object MissingSelector {
  val empty: MissingSelector = Empty

  sealed trait Color
  object B extends Color
  object R extends Color

  final class Inner private(
    private val color: Color,
    private val root: Int,
    private val left: MissingSelector,
    private val right: MissingSelector,
    val size: Int
  ) extends MissingSelector {
    override def selectAndAdd(i: Int): (Int, MissingSelector) = {
      val (newI, newTree) = selAndAdd(i)
      (newI, newTree.asBlack)
    }

    override protected def selAndAdd(i: Int): (Int, Inner) = {
      val (newI, newTree) = if (i + left.size < root) {
        val (newI, newLeft) = left.selAndAdd(i)
        (newI, Inner(color, root, newLeft, right))
      } else {
        val (newI, newRight) = right.selAndAdd(i + left.size + 1)
        (newI, Inner(color, root, left, newRight))
      }
      (newI, newTree.balance)
    }

    override def toList(others: List[Int]): List[Int] = left.toList(root :: right.toList(others))

    private def balance: Inner = this match {
      case Inner(B, z, Inner(R, y, Inner(R, x, a, b), c), d) =>
        Inner(R, y, Inner(B, x, a, b), Inner(B, z, c, d))
      case Inner(B, z, Inner(R, x, a, Inner(R, y, b, c)), d) =>
        Inner(R, y, Inner(B, x, a, b), Inner(B, z, c, d))
      case Inner(B, x, a, Inner(R, z, Inner(R, y, b, c), d)) =>
        Inner(R, y, Inner(B, x, a, b), Inner(B, z, c, d))
      case Inner(B, x, a, Inner(R, y, b, Inner(R, z, c, d))) =>
        Inner(R, y, Inner(B, x, a, b), Inner(B, z, c, d))
      case _ => this
    }

    private def asBlack: Inner = if (color == B) this else Inner(B, root, left, right)
  }

  object Inner {
    private[MissingSelector] def apply(color: Color, root: Int, left: MissingSelector, right: MissingSelector): Inner =
      new Inner(color, root, left, right, left.size + right.size + 1)
    def unapply(inner: Inner): Option[(Color, Int, MissingSelector, MissingSelector)] =
      Some((inner.color, inner.root, inner.left, inner.right))
  }

  private object Empty extends MissingSelector {
    override def selectAndAdd(i: Int): (Int, MissingSelector) = (i, Inner(R, i, Empty, Empty))
    override protected def selAndAdd(i: Int): (Int, Inner) = (i, Inner(R, i, Empty, Empty))
    override def size: Int = 0
    override def toList(others: List[Int]): List[Int] = others
  }
}