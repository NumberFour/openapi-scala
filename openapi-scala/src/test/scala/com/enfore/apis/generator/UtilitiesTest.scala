package com.enfore.apis.generator

import org.scalatest.funsuite.AnyFunSuite

class UtilitiesTest extends AnyFunSuite {
  test("cleanScalaSymbolEnum") {
    assert(Utilities.cleanScalaSymbolEnum("DE_STANDARD_VAT_2007") === "DE_STANDARD_VAT_2007")
  }

}
