package com.enfore.apis.generator

import org.scalatest.FunSuite

class UtilitiesTest extends FunSuite {
  test("cleanScalaSymbolEnum") {
    assert(Utilities.cleanScalaSymbolEnum("DE_STANDARD_VAT_2007") === "DE_STANDARD_VAT_2007")
  }

}
