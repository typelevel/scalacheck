package org.scalacheck

class PropsClass extends Properties("TestFingerprint") {
  property("propclass") = Prop.proved
}

object PropsObject extends Properties("TestFingerprint") {
  property("propobject") = Prop.proved
}
