package com.enfore.apis.ast

import com.enfore.apis.ast.SwaggerAST.{ReferenceObject, SchemaObject, SchemaObjectType, SchemaOrReferenceObject}
import org.scalatest.FunSuite

class ASTTranslationFunctionsTest extends FunSuite {

  val readOnlyString: SchemaObject = SchemaObject(`type` = Some(SchemaObjectType.`string`), readOnly = Some(true))

  def objectWithProperties(map: Map[String, SchemaOrReferenceObject]): SchemaObject = SchemaObject(
    `type` = Some(SchemaObjectType.`object`),
    properties = Some(map)
  )

  test("testSplitReadOnlyComponents - base") {
    assert(
      ASTTranslationFunctions.splitReadOnlyComponents(
        Map(
          "CustomerRole" -> objectWithProperties(Map("id" -> readOnlyString))
        )
      ) ===
        Map(
          "CustomerRole"        -> objectWithProperties(Map("id" -> readOnlyString)),
          "CustomerRoleRequest" -> objectWithProperties(Map.empty),
        )
    )
  }

  test("testSplitReadOnlyComponents - nested") {
    assert(
      ASTTranslationFunctions.splitReadOnlyComponents(
        Map(
          "CustomerRole" -> objectWithProperties(
            Map("enforeCard" -> ReferenceObject("#/components/schemas/CustomerCard"))
          ),
          "CustomerCard" -> objectWithProperties(
            Map("added" -> readOnlyString)
          )
        )
      ) ===
        Map(
          "CustomerRole" -> objectWithProperties(
            Map("enforeCard" -> ReferenceObject("#/components/schemas/CustomerCard"))
          ),
          "CustomerRoleRequest" -> objectWithProperties(
            Map("enforeCard" -> ReferenceObject("#/components/schemas/CustomerCardRequest"))
          ),
          "CustomerCard" -> objectWithProperties(
            Map("added" -> readOnlyString)
          ),
          "CustomerCardRequest" -> objectWithProperties(
            Map.empty
          )
        )
    )
  }

  test("testSplitReadOnlyComponents - super nested") {
    assert(
      ASTTranslationFunctions.splitReadOnlyComponents(
        Map(
          "CustomerRole" -> objectWithProperties(
            Map("enforeCard" -> ReferenceObject("#/components/schemas/CustomerCard"))
          ),
          "CustomerCard" -> objectWithProperties(
            Map("foo" -> ReferenceObject("#/components/schemas/FooObject"))
          ),
          "FooObject" -> objectWithProperties(
            Map("added" -> readOnlyString)
          )
        )
      ) ===
        Map(
          "CustomerRole" -> objectWithProperties(
            Map("enforeCard" -> ReferenceObject("#/components/schemas/CustomerCard"))
          ),
          "CustomerRoleRequest" -> objectWithProperties(
            Map("enforeCard" -> ReferenceObject("#/components/schemas/CustomerCardRequest"))
          ),
          "CustomerCard" -> objectWithProperties(
            Map("foo" -> ReferenceObject("#/components/schemas/FooObject"))
          ),
          "CustomerCardRequest" -> objectWithProperties(
            Map("foo" -> ReferenceObject("#/components/schemas/FooObjectRequest"))
          ),
          "FooObject" -> objectWithProperties(
            Map("added" -> readOnlyString)
          ),
          "FooObjectRequest" -> objectWithProperties(
            Map.empty
          )
        )
    )
  }
}
