package org.scalacheck

class PropClass extends Prop {
  // TODO: Give prop name when #90 is implemented
  def apply(prms: Gen.Parameters) = Prop.proved(prms)
}

object PropObject extends Prop {
  // TODO: Give prop name when #90 is implemented
  def apply(prms: Gen.Parameters) = Prop.proved(prms)
}

class PropsClass extends Properties("TestFingerprint") {
  property("propclass") = Prop.proved
}

object PropsObject extends Properties("TestFingerprint") {
  property("propobject") = Prop.proved
}
