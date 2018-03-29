package org.scalacheck

import Prop.forAll

object GenCodepointSpecification extends Properties("Gen") {
    //private use
    property("privateUseCP is defined") = forAll(Gen.privateUseCP)(Character.isDefined)
    property("privateUseCP is no control character") = forAll(Gen.privateUseCP) {cp => !Character.isISOControl(cp) }
    property("privateUseCP is no letter or digit") = forAll(Gen.privateUseCP) { cp => !Character.isLetterOrDigit(cp)}
    property("privateUseCP is not whitespace") = forAll(Gen.privateUseCP) { cp => !Character.isWhitespace(cp)}
    property("privateUseCP is in cat private use") = forAll(Gen.privateUseCP) {cp => Character.getType(cp) == Character.PRIVATE_USE}

    property("nonCharCP is not defined") = forAll(Gen.nonCharCP) { cp => !Character.isDefined(cp)}

    property("plainBMPCP is defined") = forAll(Gen.plainBMPCP)(Character.isDefined)
    property("plainBMPCP is no surrogate") = forAll(Gen.plainBMPCP)(cp => !Character.isHighSurrogate(cp.toChar) && !Character.isLowSurrogate(cp.toChar))
    property("plainBMPCP is in the BMP") = forAll(Gen.plainBMPCP)(cp => !Character.isSupplementaryCodePoint(cp))
    property("plainBMPCP is not private use") = forAll(Gen.plainBMPCP) {cp => Character.getType(cp) != Character.PRIVATE_USE}

    property("validBMPCP is no surrogate") = forAll(Gen.validBMPCP)(cp => !Character.isHighSurrogate(cp.toChar) && !Character.isLowSurrogate(cp.toChar))
    property("validBMPCP is in the BMP") = forAll(Gen.validBMPCP)(cp => !Character.isSupplementaryCodePoint(cp))

    property("emptyPlaneCP is not defined") = forAll(Gen.emptyPlaneCP){cp => !Character.isDefined(cp)} 

    property("supplementalPUCP is private use or end of block") = forAll(Gen.supplementalPUCP){cp => (
        (Character.getType(cp) == Character.PRIVATE_USE) ||
        (cp == 0xFFFFF || cp == 0xFFFFE || cp == 0x10FFFF || cp == 0x10FFFE)
    )}

}