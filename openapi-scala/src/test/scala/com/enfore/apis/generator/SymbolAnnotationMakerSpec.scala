package com.enfore.apis.generator

import cats.data.NonEmptyList
import com.enfore.apis.repr.TypeRepr.{MaxLength, MinLength, PrimitiveString}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SymbolAnnotationMakerSpec extends AnyFlatSpec with Matchers {

  "dataRefinementMatcher" should "generate proper type signatures for refined primitives" in {
    val op: String =
      SymbolAnnotationMaker.primitiveTypeSigWithRefinements(
        PrimitiveString(Some(NonEmptyList.of(MinLength(1), MaxLength(256))), None)
      )

    op shouldBe "String Refined AllOf[MinSize[W.`1`.T] :: MaxSize[W.`256`.T] :: HNil]"
  }

}
