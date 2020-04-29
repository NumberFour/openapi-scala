package com.enfore.apis.ast

import com.enfore.apis.ast.ASTTranslationFunctions.PackageName
import com.enfore.apis.ast.SwaggerAST._
import org.scalatest.funsuite.AnyFunSuite

class ASTTranslationFunctionsTest extends AnyFunSuite {

  val readOnlyString: SchemaObject = SchemaObject(`type` = Some(SchemaObjectType.`string`), readOnly = Some(true))

  def objectWithProperties(map: Map[String, SchemaOrReferenceObject]): SchemaObject = SchemaObject(
    `type` = Some(SchemaObjectType.`object`),
    properties = Some(map)
  )

  test("routeDefFromSwaggerAST - fails upon bad parameter patterns") {
    assertThrows[AssertionError](
      ASTTranslationFunctions.routeDefFromSwaggerAST("/invoices/{invoice-id}")(
        OperationObject(
          summary = None,
          description = None,
          operationId = None,
          requestBody = None,
          responses = Map.empty,
          parameters = Some(
            List(
              ParameterObject(
                name = "invoice_id",
                in = ParameterLocation.path,
                schema = None,
                description = None,
                required = Some(true),
                deprecated = None,
                allowEmptyValue = None,
                content = None
              )
            )
          )
        ),
        RequestType.POST
      )(Map.empty)(PackageName("test"))
    )
  }

  test("testSplitReadOnlyComponents - base") {
    assert(
      ASTTranslationFunctions.splitReadOnlyComponents(
        Map(
          "CustomerRole" -> objectWithProperties(Map("id" -> readOnlyString))
        )
      ) ===
        Map(
          "CustomerRole"        -> objectWithProperties(Map("id" -> readOnlyString)),
          "CustomerRoleRequest" -> objectWithProperties(Map.empty)
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
